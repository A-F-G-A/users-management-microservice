package com.jcaa.usersmanagement.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcaa.usersmanagement.application.port.in.CellphoneCommandUseCase;
import com.jcaa.usersmanagement.application.port.in.CellphoneQueryUseCase;
import com.jcaa.usersmanagement.application.port.out.CellphoneReadPort;
import com.jcaa.usersmanagement.application.port.out.CellphoneWritePort;
import com.jcaa.usersmanagement.application.port.out.SaveUserPort;
import com.jcaa.usersmanagement.application.port.out.UpdateUserPort;
import com.jcaa.usersmanagement.application.service.BootstrapAdminService;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.exception.CellphoneAlreadyExistsException;
import com.jcaa.usersmanagement.domain.exception.CellphoneNotFoundException;
import com.jcaa.usersmanagement.domain.model.CellphoneModel;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * End-to-end over a real MySQL 8.4 with Flyway enabled: HTTP + JWT + Spring Security + CQRS
 * handlers + JDBC adapters + SQL. No JdbcTemplate/ResultSet mocks.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
      "spring.main.web-application-type=servlet",
      "spring.flyway.enabled=true",
      "spring.flyway.baseline-on-migrate=true",
      "spring.flyway.baseline-version=1",
      "bootstrap.admin.name=Admin IT",
      "bootstrap.admin.email=" + CellphoneMySqlIT.ADMIN_EMAIL,
      "bootstrap.admin.password=" + CellphoneMySqlIT.ADMIN_PASSWORD
    })
@AutoConfigureMockMvc
@EnabledIf(value = "com.jcaa.usersmanagement.it.MySqlTestServer#isAvailable", disabledReason = "Sin Docker ni IT_MYSQL_HOST: pruebas MySQL omitidas")
@DisplayName("Celulares sobre MySQL 8.4 real")
class CellphoneMySqlIT {

  static final String ADMIN_EMAIL = "admin.it@example.com";
  static final String ADMIN_PASSWORD = "it-only-admin-password";
  private static final String USER_PASSWORD = "it-only-user-password";
  private static final AtomicLong IMEI_SEQUENCE = new AtomicLong(3569380356400L);

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private CellphoneReadPort readPort;
  @Autowired private CellphoneWritePort writePort;
  @Autowired private CellphoneCommandUseCase commandUseCase;
  @Autowired private CellphoneQueryUseCase queryUseCase;
  @Autowired private SaveUserPort saveUserPort;
  @Autowired private UpdateUserPort updateUserPort;
  @Autowired private BootstrapAdminService bootstrapAdminService;

  @BeforeAll
  static void startServer() {
    MySqlTestServer.startOrSkip();
  }

  @DynamicPropertySource
  static void database(final DynamicPropertyRegistry registry) {
    final String database = MySqlTestServer.createDatabase("it_app");
    registry.add("db.host", MySqlTestServer::host);
    registry.add("db.port", MySqlTestServer::port);
    registry.add("db.name", () -> database);
    registry.add("db.username", MySqlTestServer::user);
    registry.add("db.password", MySqlTestServer::password);
    registry.add("db.ssl-mode", () -> "DISABLED");
  }

  @BeforeEach
  void cleanCellphones() {
    jdbc.update("DELETE FROM cellphones");
  }

  // ── Flyway y administrador inicial ─────────────────────────────────────────

  @Test
  @DisplayName("Flyway migra al arrancar y el primer ADMIN se crea una sola vez")
  void shouldMigrateAndBootstrapExactlyOneAdmin() {
    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class)).isEqualTo(2);
    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'", Integer.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject(
        "SELECT password FROM users WHERE email = ?", String.class, ADMIN_EMAIL))
        .startsWith("$2").isNotEqualTo(ADMIN_PASSWORD);

    assertThat(bootstrapAdminService.createFirstAdminIfNoneExists(
        "Otro", "otro.admin@example.com", "another-long-password")).isFalse();
    assertThat(jdbc.queryForObject(
        "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'", Integer.class)).isEqualTo(1);
  }

  @Test
  @DisplayName("OpenAPI publica la versión del pom y los endpoints de celulares")
  void shouldPublishOpenApiWithBuildVersion(
      @Autowired final org.springframework.boot.info.BuildProperties buildProperties) throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info.version").value(buildProperties.getVersion()))
        .andExpect(jsonPath("$.paths['/api/cellphones'].get").exists())
        .andExpect(jsonPath("$.paths['/api/cellphones/{id}'].put").exists());
  }

  // ── CRUDL completo REST → dominio → SQL → dominio → REST ───────────────────

  @Test
  @DisplayName("CRUDL completo como ADMIN conserva los 21 atributos")
  void shouldRunFullCrudlRoundTrip() throws Exception {
    final String token = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    final String imei = nextImei("0");

    final JsonNode created = json(mockMvc.perform(post("/api/cellphones").header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON).content(body(imei, "Acme", null)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString());
    final String id = created.get("id").asText();
    assertThat(UUID.fromString(id)).isNotNull();

    final Map<String, Object> row = jdbc.queryForMap("SELECT * FROM cellphones WHERE id = ?", id);
    assertThat(row.get("imei")).isEqualTo(imei);
    assertThat(row.get("brand")).isEqualTo("Acme");
    assertThat(row.get("operator_name")).isEqualTo("Carrier");
    assertThat(row.get("secondary_storage")).isNull();
    assertThat((BigDecimal) row.get("screen_size")).isEqualByComparingTo("6.20");
    assertThat((BigDecimal) row.get("megapixels")).isEqualByComparingTo("48.5");
    assertThat((BigDecimal) row.get("cpu_speed")).isEqualByComparingTo("2.45");
    assertThat(row.get("wifi")).isEqualTo(true);
    assertThat(row.get("bluetooth")).isEqualTo(false);

    final JsonNode fetched = json(mockMvc.perform(get("/api/cellphones/{id}", id).header("Authorization", token))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    assertThat(fetched).isEqualTo(created);
    assertThat(fetched.get("imei").asText()).isEqualTo(imei).startsWith("0");
    assertThat(fetched.get("marca").asText()).isEqualTo("Acme");
    assertThat(fetched.get("pulgadas").decimalValue()).isEqualByComparingTo("6.2");
    assertThat(fetched.get("megapixeles").decimalValue()).isEqualByComparingTo("48.5");
    assertThat(fetched.get("ram").asInt()).isEqualTo(8);
    assertThat(fetched.get("almacenamientoPrincipal").asInt()).isEqualTo(128);
    assertThat(fetched.get("almacenamientoSecundario").isNull()).isTrue();
    assertThat(fetched.get("sistemaOperativo").asText()).isEqualTo("Android");
    assertThat(fetched.get("operador").asText()).isEqualTo("Carrier");
    assertThat(fetched.get("tecnologiaBanda").asText()).isEqualTo("5G");
    assertThat(fetched.get("wifi").asBoolean()).isTrue();
    assertThat(fetched.get("bluetooth").asBoolean()).isFalse();
    assertThat(fetched.get("camaras").asInt()).isEqualTo(3);
    assertThat(fetched.get("marcaCpu").asText()).isEqualTo("Acme CPU");
    assertThat(fetched.get("velocidadCpu").decimalValue()).isEqualByComparingTo("2.45");
    assertThat(fetched.get("nfc").asBoolean()).isTrue();
    assertThat(fetched.get("huella").asBoolean()).isFalse();
    assertThat(fetched.get("ir").asBoolean()).isTrue();
    assertThat(fetched.get("resisteAgua").asBoolean()).isFalse();
    assertThat(fetched.get("cantidadSim").asInt()).isEqualTo(2);

    mockMvc.perform(get("/api/cellphones").header("Authorization", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(id));

    mockMvc.perform(put("/api/cellphones/{id}", id).header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON).content(body(imei, "Nova", 256)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.marca").value("Nova"))
        .andExpect(jsonPath("$.almacenamientoSecundario").value(256));
    assertThat(jdbc.queryForObject("SELECT brand FROM cellphones WHERE id = ?", String.class, id))
        .isEqualTo("Nova");

    // Same values again: MySQL reports matched rows, so this must not become a false 404.
    mockMvc.perform(put("/api/cellphones/{id}", id).header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON).content(body(imei, "Nova", 256)))
        .andExpect(status().isOk());

    mockMvc.perform(delete("/api/cellphones/{id}", id).header("Authorization", token))
        .andExpect(status().isNoContent());
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cellphones", Integer.class)).isZero();
    mockMvc.perform(get("/api/cellphones/{id}", id).header("Authorization", token))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("409 por IMEI duplicado al crear y al actualizar; 404 en ids inexistentes")
  void shouldMapConflictsAndMissingIds() throws Exception {
    final String token = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    final String imeiA = nextImei("3");
    final String imeiB = nextImei("3");
    final String idB = createViaApi(token, imeiB);
    createViaApi(token, imeiA);

    mockMvc.perform(post("/api/cellphones").header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON).content(body(imeiA, "Copy", null)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Ya existe un celular con IMEI '" + imeiA + "'."));
    mockMvc.perform(put("/api/cellphones/{id}", idB).header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON).content(body(imeiA, "Copy", null)))
        .andExpect(status().isConflict());
    assertThat(jdbc.queryForObject("SELECT imei FROM cellphones WHERE id = ?", String.class, idB))
        .as("la actualización rechazada no modifica la fila").isEqualTo(imeiB);

    final String missing = UUID.randomUUID().toString();
    mockMvc.perform(get("/api/cellphones/{id}", missing).header("Authorization", token))
        .andExpect(status().isNotFound());
    mockMvc.perform(put("/api/cellphones/{id}", missing).header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON).content(body(nextImei("3"), "X", null)))
        .andExpect(status().isNotFound());
    mockMvc.perform(delete("/api/cellphones/{id}", missing).header("Authorization", token))
        .andExpect(status().isNotFound());
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cellphones", Integer.class)).isEqualTo(2);
  }

  @Test
  @DisplayName("Luhn inválido y JSON mal formado responden 400 y no escriben en la base")
  void shouldRejectInvalidInputWithoutTouchingDatabase() throws Exception {
    final String token = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    mockMvc.perform(post("/api/cellphones").header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON).content(body("012345678901234", "Acme", null)))
        .andExpect(status().isBadRequest());
    mockMvc.perform(post("/api/cellphones").header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"marca\":"))
        .andExpect(status().isBadRequest());
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cellphones", Integer.class)).isZero();
  }

  // ── Permisos con usuarios reales y JWT reales ──────────────────────────────

  @Test
  @DisplayName("REVIEWER lee; REVIEWER y MEMBER no escriben; sin token 401")
  void shouldEnforceRolesWithRealTokens() throws Exception {
    final String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    final String reviewer = login(activeUser(UserRole.REVIEWER).getEmail().value(), USER_PASSWORD);
    final String member = login(activeUser(UserRole.MEMBER).getEmail().value(), USER_PASSWORD);
    final String id = createViaApi(admin, nextImei("5"));

    mockMvc.perform(get("/api/cellphones").header("Authorization", reviewer)).andExpect(status().isOk());
    mockMvc.perform(get("/api/cellphones/{id}", id).header("Authorization", reviewer))
        .andExpect(status().isOk());
    mockMvc.perform(post("/api/cellphones").header("Authorization", reviewer)
            .contentType(MediaType.APPLICATION_JSON).content(body(nextImei("5"), "R", null)))
        .andExpect(status().isForbidden());
    mockMvc.perform(delete("/api/cellphones/{id}", id).header("Authorization", reviewer))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/cellphones").header("Authorization", member))
        .andExpect(status().isForbidden());
    mockMvc.perform(put("/api/cellphones/{id}", id).header("Authorization", member)
            .contentType(MediaType.APPLICATION_JSON).content(body(nextImei("5"), "M", null)))
        .andExpect(status().isForbidden());
    mockMvc.perform(delete("/api/cellphones/{id}", id)).andExpect(status().isUnauthorized());
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cellphones", Integer.class)).isEqualTo(1);
  }

  @Test
  @DisplayName("Registro público con role=ADMIN queda MEMBER/PENDING en MySQL")
  void shouldNotEscalatePrivilegesOnPublicRegistration() throws Exception {
    final String email = "mallory." + UUID.randomUUID() + "@example.com";
    final String clientId = "00000000-0000-0000-0000-00000000dead";
    mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
            {"id":"%s","name":"Mallory","email":"%s","password":"%s","role":"ADMIN","status":"ACTIVE"}
            """.formatted(clientId, email, USER_PASSWORD)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.role").value("MEMBER"));

    final Map<String, Object> row = jdbc.queryForMap("SELECT id, role, status FROM users WHERE email = ?", email);
    assertThat(row.get("role")).isEqualTo("MEMBER");
    assertThat(row.get("status")).isEqualTo("PENDING");
    assertThat(row.get("id")).isNotEqualTo(clientId);
    mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, USER_PASSWORD)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("El token de un ADMIN desactivado deja de servir inmediatamente")
  void shouldRevokeAccessOfDeactivatedAdmin() throws Exception {
    final UserModel admin = activeUser(UserRole.ADMIN);
    final String token = login(admin.getEmail().value(), USER_PASSWORD);
    mockMvc.perform(get("/api/cellphones").header("Authorization", token)).andExpect(status().isOk());

    updateUserPort.update(admin.deactivate());

    mockMvc.perform(get("/api/cellphones").header("Authorization", token))
        .andExpect(status().isUnauthorized());
    mockMvc.perform(post("/api/cellphones").header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON).content(body(nextImei("7"), "X", null)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Regresión Usuario: registro → ADMIN activa y promueve → login → eliminación")
  void shouldKeepUserModuleWorkingOnMySql() throws Exception {
    final String admin = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    final String email = "ana." + UUID.randomUUID() + "@example.com";
    final String id = json(mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Ana Gómez\",\"email\":\"%s\",\"password\":\"%s\"}"
                .formatted(email, USER_PASSWORD)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString()).get("id").asText();

    mockMvc.perform(put("/api/users/{id}", id).header("Authorization", admin)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Ana Gómez\",\"email\":\"%s\",\"role\":\"REVIEWER\",\"status\":\"ACTIVE\"}"
                .formatted(email)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("REVIEWER"));
    final String reviewer = login(email, USER_PASSWORD);
    mockMvc.perform(get("/api/users/{id}", id).header("Authorization", reviewer))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Ana Gómez"));
    mockMvc.perform(delete("/api/users/{id}", id).header("Authorization", reviewer))
        .andExpect(status().isForbidden());

    mockMvc.perform(delete("/api/users/{id}", id).header("Authorization", admin))
        .andExpect(status().isNoContent());
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, id)).isZero();
    mockMvc.perform(get("/api/users/{id}", id).header("Authorization", admin))
        .andExpect(status().isNotFound());
  }

  // ── Repositorios de lectura/escritura y transacciones ──────────────────────

  @Test
  @DisplayName("Puertos de lectura y escritura ejecutan INSERT, SELECT, UPDATE y DELETE reales")
  void shouldPersistThroughReadAndWritePorts() {
    final CellphoneModel model = model(nextImei("1"));

    writePort.save(model);
    assertThat(readPort.getById(model.id())).contains(model);
    assertThat(readPort.getAll()).containsExactly(model);

    final CellphoneModel changed = model.toBuilder().brand("Changed").secondaryStorage(64)
        .wifi(false).cpuSpeed(new BigDecimal("3.10")).build();
    writePort.update(changed);
    final CellphoneModel reloaded = readPort.getById(model.id()).orElseThrow();
    assertThat(reloaded.brand()).isEqualTo("Changed");
    assertThat(reloaded.secondaryStorage()).isEqualTo(64);
    assertThat(reloaded.wifi()).isFalse();
    assertThat(reloaded.cpuSpeed()).isEqualByComparingTo("3.1");

    writePort.delete(model.id());
    assertThat(readPort.getById(model.id())).isEmpty();
    assertThatThrownBy(() -> writePort.delete(model.id())).isInstanceOf(CellphoneNotFoundException.class);
    assertThatThrownBy(() -> writePort.update(changed)).isInstanceOf(CellphoneNotFoundException.class);
  }

  @Test
  @DisplayName("Rollback: si una operación falla dentro de la transacción no queda nada escrito")
  void shouldRollBackWholeTransactionOnFailure() {
    final CellphoneModel first = model(nextImei("2"));
    final CellphoneModel duplicate = model(first.imei());

    assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
      writePort.save(first);
      assertThat(readPort.getById(first.id())).as("visible dentro de la transacción").isPresent();
      writePort.save(duplicate);
    })).isInstanceOf(CellphoneAlreadyExistsException.class);

    assertThat(readPort.getById(first.id())).as("revertido tras el fallo").isEmpty();
    assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM cellphones", Integer.class)).isZero();
  }

  @Test
  @DisplayName("Los handlers CQRS están envueltos por proxies transaccionales")
  void shouldApplyTransactionalProxies() {
    assertThat(AopUtils.isAopProxy(commandUseCase)).isTrue();
    assertThat(AopUtils.isAopProxy(queryUseCase)).isTrue();
    final CellphoneModel created = commandUseCase.create(
        com.jcaa.usersmanagement.application.service.dto.command.CreateCellphoneCommand.builder()
            .brand("Acme").imei(nextImei("4")).screenSize(new BigDecimal("6.1"))
            .megapixels(BigDecimal.TEN).ram(4).primaryStorage(64).operatingSystem("Android")
            .operator("Carrier").bandTechnology("4G").wifi(true).bluetooth(true).cameras(1)
            .cpuBrand("CPU").cpuSpeed(BigDecimal.ONE).nfc(false).fingerprint(false)
            .infrared(false).waterResistant(false).simCount(1).build());
    assertThat(queryUseCase.getAll()).extracting(CellphoneModel::id).containsExactly(created.id());
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private String login(final String email, final String password) throws Exception {
    final String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    return "Bearer " + json(response).get("accessToken").asText();
  }

  private UserModel activeUser(final UserRole role) {
    final UserModel user = UserModel.create(
            new UserId(UUID.randomUUID().toString()),
            new UserName("IT " + role.name()),
            new UserEmail(role.name().toLowerCase() + "." + UUID.randomUUID() + "@example.com"),
            UserPassword.fromPlainText(USER_PASSWORD),
            role)
        .activate();
    return saveUserPort.save(user);
  }

  private String createViaApi(final String token, final String imei) throws Exception {
    return json(mockMvc.perform(post("/api/cellphones").header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON).content(body(imei, "Acme", null)))
        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString())
        .get("id").asText();
  }

  private JsonNode json(final String content) throws Exception {
    return objectMapper.readTree(content);
  }

  private static String body(final String imei, final String brand, final Integer secondaryStorage) {
    return """
        {"marca":"%s","imei":"%s","pulgadas":6.20,"megapixeles":48.5,"ram":8,
         "almacenamientoPrincipal":128,"almacenamientoSecundario":%s,"sistemaOperativo":"Android",
         "operador":"Carrier","tecnologiaBanda":"5G","wifi":true,"bluetooth":false,"camaras":3,
         "marcaCpu":"Acme CPU","velocidadCpu":2.45,"nfc":true,"huella":false,"ir":true,
         "resisteAgua":false,"cantidadSim":2}
        """.formatted(brand, imei, secondaryStorage);
  }

  private static CellphoneModel model(final String imei) {
    return CellphoneModel.builder()
        .id(UUID.randomUUID().toString()).brand("Acme").imei(imei)
        .screenSize(new BigDecimal("6.20")).megapixels(new BigDecimal("48.00")).ram(8)
        .primaryStorage(128).secondaryStorage(null).operatingSystem("Android").operator("Carrier")
        .bandTechnology("5G").wifi(true).bluetooth(false).cameras(3).cpuBrand("Acme CPU")
        .cpuSpeed(new BigDecimal("2.40")).nfc(true).fingerprint(false).infrared(true)
        .waterResistant(false).simCount(2).build();
  }

  /** Unique Luhn-valid IMEI; {@code firstDigit} "0" exercises leading-zero preservation. */
  private static String nextImei(final String firstDigit) {
    final String body = (firstDigit + IMEI_SEQUENCE.incrementAndGet()).substring(0, 14);
    for (int check = 0; check <= 9; check++) {
      final String candidate = body + check;
      int sum = 0;
      for (int i = 0; i < 15; i++) {
        int digit = candidate.charAt(14 - i) - '0';
        if (i % 2 == 1 && (digit *= 2) > 9) digit -= 9;
        sum += digit;
      }
      if (sum % 10 == 0) return candidate;
    }
    throw new IllegalStateException("unreachable");
  }
}
