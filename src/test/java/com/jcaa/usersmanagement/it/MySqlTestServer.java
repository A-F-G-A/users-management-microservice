package com.jcaa.usersmanagement.it;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;

/**
 * Real MySQL 8.4 shared by all *IT classes. Uses Testcontainers when Docker is available (CI);
 * otherwise an already running, disposable server given by IT_MYSQL_HOST, IT_MYSQL_PORT,
 * IT_MYSQL_USER and IT_MYSQL_PASSWORD (a user allowed to CREATE DATABASE). Without either, the
 * integration tests are reported as skipped, never as passed.
 */
public final class MySqlTestServer {

  public static final String IMAGE = "mysql:8.4";

  private static MySQLContainer<?> container;
  private static String host;
  private static int port;
  private static String user;
  private static String password;

  private MySqlTestServer() {}

  /**
   * JUnit @EnabledIf condition. With IT_REQUIRE_MYSQL=true (set in CI) it always returns true, so a
   * missing database fails the build instead of silently skipping the integration tests.
   */
  public static boolean isAvailable() {
    return "true".equalsIgnoreCase(System.getenv("IT_REQUIRE_MYSQL"))
        || hasText(System.getenv("IT_MYSQL_HOST"))
        || DockerClientFactory.instance().isDockerAvailable();
  }

  private static boolean hasText(final String value) {
    return value != null && !value.isBlank();
  }

  public static synchronized void startOrSkip() {
    if (host != null) return;
    final String externalHost = System.getenv("IT_MYSQL_HOST");
    if (hasText(externalHost)) {
      host = externalHost;
      port = Integer.parseInt(System.getenv().getOrDefault("IT_MYSQL_PORT", "3306"));
      user = System.getenv().getOrDefault("IT_MYSQL_USER", "root");
      password = System.getenv().getOrDefault("IT_MYSQL_PASSWORD", "");
      return;
    }
    assumeTrue(
        "true".equalsIgnoreCase(System.getenv("IT_REQUIRE_MYSQL"))
            || DockerClientFactory.instance().isDockerAvailable(),
        "Docker no disponible y sin IT_MYSQL_HOST: se omiten las pruebas de integración MySQL.");
    container = new MySQLContainer<>(IMAGE).withUsername("root").withPassword("it-only-root");
    container.start();
    host = container.getHost();
    port = container.getMappedPort(MySQLContainer.MYSQL_PORT);
    user = container.getUsername();
    password = container.getPassword();
  }

  /** Creates an empty, uniquely named database so every test class starts from scratch. */
  public static String createDatabase(final String prefix) {
    final String name = prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    try (Connection connection = DriverManager.getConnection(serverUrl(), user, password);
        Statement statement = connection.createStatement()) {
      statement.execute(
          "CREATE DATABASE " + name + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    } catch (final SQLException exception) {
      throw new IllegalStateException("No se pudo crear la base de pruebas " + name, exception);
    }
    return name;
  }

  public static DataSource dataSource(final String database) {
    final HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(jdbcUrl(database));
    dataSource.setUsername(user);
    dataSource.setPassword(password);
    dataSource.setMaximumPoolSize(2);
    return dataSource;
  }

  public static String jdbcUrl(final String database) {
    return "jdbc:mysql://%s:%d/%s?sslMode=DISABLED&serverTimezone=UTC&allowPublicKeyRetrieval=true"
        .formatted(host, port, database);
  }

  private static String serverUrl() {
    return jdbcUrl("");
  }

  public static String host() {
    return host;
  }

  public static int port() {
    return port;
  }

  public static String user() {
    return user;
  }

  public static String password() {
    return password;
  }
}
