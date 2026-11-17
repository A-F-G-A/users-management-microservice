package com.jcaa.usersmanagement.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Flyway migrations against a real MySQL 8.4: fresh installs and pre-Flyway schemas. */
@EnabledIf(value = "com.jcaa.usersmanagement.it.MySqlTestServer#isAvailable", disabledReason = "Sin Docker ni IT_MYSQL_HOST: pruebas MySQL omitidas")
@DisplayName("Flyway + MySQL 8.4")
class FlywayMigrationIT {

  @BeforeAll
  static void startServer() {
    MySqlTestServer.startOrSkip();
  }

  /** Same settings as src/main/resources/application.properties (spring.flyway.*). */
  private static Flyway flyway(final DataSource dataSource) {
    return Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .baselineOnMigrate(true)
        .baselineVersion("1")
        .load();
  }

  @Test
  @DisplayName("Base nueva: aplica V1 y V2, valida y es idempotente")
  void shouldMigrateFreshDatabase() {
    final DataSource dataSource = MySqlTestServer.dataSource(MySqlTestServer.createDatabase("it_fresh"));
    final Flyway flyway = flyway(dataSource);

    final MigrateResult result = flyway.migrate();

    assertThat(result.success).isTrue();
    assertThat(result.migrationsExecuted).isEqualTo(2);
    assertThat(Arrays.stream(flyway.info().applied()).map(MigrationInfo::getVersion).map(Object::toString))
        .containsExactly("1", "2");
    flyway.validate();
    assertThat(flyway.migrate().migrationsExecuted).isZero();

    final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    assertThat(columns(jdbc, "users"))
        .contains("id", "name", "email", "password", "role", "status", "created_at", "updated_at");
    assertThat(columns(jdbc, "cellphones")).hasSize(21)
        .contains("imei", "operator_name", "secondary_storage", "water_resistant", "sim_count");
    assertThat(jdbc.queryForObject(
        "SELECT DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() "
            + "AND TABLE_NAME = 'cellphones' AND COLUMN_NAME = 'imei'", String.class))
        .isEqualTo("varchar");
    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() "
            + "AND TABLE_NAME = 'cellphones' AND CONSTRAINT_NAME = 'uq_cellphones_imei' "
            + "AND CONSTRAINT_TYPE = 'UNIQUE'", Integer.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class))
        .as("las migraciones no deben sembrar usuarios ni credenciales").isZero();
  }

  @Test
  @DisplayName("Esquema existente sin Flyway (schema.sql): baseline en V1, aplica V2 y conserva datos")
  void shouldBaselineExistingSchemaAndKeepData() throws IOException {
    final DataSource dataSource = MySqlTestServer.dataSource(MySqlTestServer.createDatabase("it_legacy"));
    final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    legacySchemaStatements().forEach(jdbc::execute);
    jdbc.update("INSERT INTO users (id, name, email, password, role, status) VALUES (?, ?, ?, ?, ?, ?)",
        "legacy-1", "Legacy User", "legacy@example.com", "$2a$12$legacyhashlegacyhashlegacyhashlegacyhashlegacyhash",
        "MEMBER", "ACTIVE");

    final MigrateResult result = flyway(dataSource).migrate();

    assertThat(result.success).isTrue();
    assertThat(result.migrationsExecuted).isEqualTo(1);
    final List<Map<String, Object>> history = jdbc.queryForList(
        "SELECT version, type, success FROM flyway_schema_history ORDER BY installed_rank");
    assertThat(history).extracting(row -> row.get("version")).containsExactly("1", "2");
    assertThat(history.get(0).get("type")).isEqualTo("BASELINE");
    assertThat(jdbc.queryForObject("SELECT email FROM users WHERE id = 'legacy-1'", String.class))
        .isEqualTo("legacy@example.com");
    assertThat(columns(jdbc, "cellphones")).hasSize(21);
    flyway(dataSource).validate();
  }

  @Test
  @DisplayName("La restricción única de IMEI existe en la base real")
  void shouldEnforceImeiUniquenessAtDatabaseLevel() {
    final DataSource dataSource = MySqlTestServer.dataSource(MySqlTestServer.createDatabase("it_unique"));
    flyway(dataSource).migrate();
    final JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    final String insert = "INSERT INTO cellphones VALUES (?, 'Acme', '012345678901237', 6.2, 48, 8, 128, "
        + "NULL, 'Android', 'Carrier', '5G', 0, 0, 3, 'CPU', 2.4, 0, 0, 0, 0, 2)";
    jdbc.update(insert, "c-1");

    assertThatThrownBy(() -> jdbc.update(insert, "c-2"))
        .isInstanceOf(DuplicateKeyException.class)
        .hasMessageContaining("uq_cellphones_imei");
  }

  private static List<String> columns(final JdbcTemplate jdbc, final String table) {
    return jdbc.queryForList(
        "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() "
            + "AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION", String.class, table);
  }

  /** The CREATE TABLE statements of the legacy bootstrap script, without CREATE DATABASE/USE. */
  private static List<String> legacySchemaStatements() throws IOException {
    try (InputStream input = FlywayMigrationIT.class.getResourceAsStream("/schema.sql")) {
      assertThat(input).as("schema.sql en el classpath").isNotNull();
      final String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8)
          .lines().filter(line -> !line.trim().startsWith("--")).reduce("", (a, b) -> a + "\n" + b);
      return Arrays.stream(sql.split(";"))
          .map(String::trim)
          .filter(statement -> !statement.isEmpty())
          .filter(statement -> !statement.toUpperCase().startsWith("CREATE DATABASE"))
          .filter(statement -> !statement.toUpperCase().startsWith("USE "))
          .toList();
    }
  }
}
