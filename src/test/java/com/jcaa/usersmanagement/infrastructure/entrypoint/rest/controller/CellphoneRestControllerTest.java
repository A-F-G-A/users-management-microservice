package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jcaa.usersmanagement.application.port.in.CellphoneCommandUseCase;
import com.jcaa.usersmanagement.application.port.in.CellphoneQueryUseCase;
import com.jcaa.usersmanagement.domain.exception.CellphoneAlreadyExistsException;
import com.jcaa.usersmanagement.domain.exception.CellphoneNotFoundException;
import com.jcaa.usersmanagement.domain.exception.InvalidCellphoneException;
import com.jcaa.usersmanagement.domain.model.CellphoneModel;
import org.springframework.dao.DuplicateKeyException;
import com.jcaa.usersmanagement.infrastructure.config.OpenApiConfig;
import com.jcaa.usersmanagement.application.port.out.GetUserByIdPort;
import com.jcaa.usersmanagement.infrastructure.security.JwtAuthenticationFilter;
import com.jcaa.usersmanagement.infrastructure.security.JwtPrincipal;
import com.jcaa.usersmanagement.infrastructure.security.JwtTokenService;
import com.jcaa.usersmanagement.infrastructure.security.RestAccessDeniedHandler;
import com.jcaa.usersmanagement.infrastructure.security.RestAuthenticationEntryPoint;
import com.jcaa.usersmanagement.infrastructure.security.SecurityConfig;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;

@WebMvcTest(controllers = CellphoneRestController.class,
    properties = "spring.main.web-application-type=servlet")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class,
    RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, OpenApiConfig.class,
    SpringDocConfiguration.class, SpringDocWebMvcConfiguration.class})
@EnableConfigurationProperties(SpringDocConfigProperties.class)
class CellphoneRestControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private CellphoneCommandUseCase commandUseCase;
  @MockBean private CellphoneQueryUseCase queryUseCase;
  @MockBean private JwtTokenService jwtTokenService;
  @MockBean private GetUserByIdPort getUserByIdPort;

  @Test
  void shouldRequireAuthenticationForList() throws Exception {
    mockMvc.perform(get("/api/cellphones")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "REVIEWER")
  void shouldAllowReviewerToRead() throws Exception {
    when(queryUseCase.getAll()).thenReturn(List.of());
    mockMvc.perform(get("/api/cellphones")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "REVIEWER")
  void shouldGetCellphoneById() throws Exception {
    when(queryUseCase.getById(any())).thenReturn(cellphone());
    mockMvc.perform(get("/api/cellphones/cell-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("cell-1"));
  }

  @Test
  @WithMockUser(roles = "REVIEWER")
  void shouldDenyReviewerWrites() throws Exception {
    mockMvc.perform(post("/api/cellphones").contentType(MediaType.APPLICATION_JSON)
        .content(validRequest())).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void shouldCreateCellphoneAndReturnSpanishAttributes() throws Exception {
    when(commandUseCase.create(any())).thenReturn(cellphone());
    mockMvc.perform(post("/api/cellphones").contentType(MediaType.APPLICATION_JSON)
        .content(validRequest()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.marca").value("Acme"))
        .andExpect(jsonPath("$.imei").value("012345678901237"))
        .andExpect(jsonPath("$.wifi").value(false));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void shouldRejectInvalidImei() throws Exception {
    mockMvc.perform(post("/api/cellphones").contentType(MediaType.APPLICATION_JSON)
        .content(validRequest().replace("012345678901237", "123")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldExposeCellphoneOperationsInOpenApi() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/cellphones'].post").exists())
        .andExpect(jsonPath("$.paths['/api/cellphones/{id}'].delete").exists())
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void shouldUpdateCellphone() throws Exception {
    when(commandUseCase.update(any())).thenReturn(cellphone());
    mockMvc.perform(put("/api/cellphones/cell-1").contentType(MediaType.APPLICATION_JSON)
        .content(validRequest()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.marca").value("Acme"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void shouldDeleteCellphone() throws Exception {
    mockMvc.perform(delete("/api/cellphones/cell-1")).andExpect(status().isNoContent());
    verify(commandUseCase).delete(org.mockito.ArgumentMatchers.any());
  }

  // ── Autenticación y autorización ───────────────────────────────────────────

  @Test
  void shouldReturn401ForWritesWithoutAuthentication() throws Exception {
    mockMvc.perform(post("/api/cellphones").contentType(MediaType.APPLICATION_JSON)
        .content(validRequest())).andExpect(status().isUnauthorized());
    mockMvc.perform(put("/api/cellphones/cell-1").contentType(MediaType.APPLICATION_JSON)
        .content(validRequest())).andExpect(status().isUnauthorized());
    mockMvc.perform(delete("/api/cellphones/cell-1")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/cellphones/cell-1")).andExpect(status().isUnauthorized());
    verifyNoInteractions(commandUseCase, queryUseCase);
  }

  @Test
  void shouldReturn401ForInvalidBearerToken() throws Exception {
    when(jwtTokenService.parse("forged")).thenThrow(new io.jsonwebtoken.MalformedJwtException("bad"));
    mockMvc.perform(get("/api/cellphones").header("Authorization", "Bearer forged"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "MEMBER")
  void shouldReturn403ForMemberOnEveryOperation() throws Exception {
    mockMvc.perform(get("/api/cellphones")).andExpect(status().isForbidden());
    mockMvc.perform(get("/api/cellphones/cell-1")).andExpect(status().isForbidden());
    mockMvc.perform(post("/api/cellphones").contentType(MediaType.APPLICATION_JSON)
        .content(validRequest())).andExpect(status().isForbidden());
    mockMvc.perform(put("/api/cellphones/cell-1").contentType(MediaType.APPLICATION_JSON)
        .content(validRequest())).andExpect(status().isForbidden());
    mockMvc.perform(delete("/api/cellphones/cell-1")).andExpect(status().isForbidden());
    verifyNoInteractions(commandUseCase, queryUseCase);
  }

  @Test
  @WithMockUser(roles = "REVIEWER")
  void shouldReturn403ForReviewerUpdateAndDelete() throws Exception {
    mockMvc.perform(put("/api/cellphones/cell-1").contentType(MediaType.APPLICATION_JSON)
        .content(validRequest())).andExpect(status().isForbidden());
    mockMvc.perform(delete("/api/cellphones/cell-1")).andExpect(status().isForbidden());
    verifyNoInteractions(commandUseCase);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void shouldAllowAdminToRead() throws Exception {
    when(queryUseCase.getAll()).thenReturn(List.of(cellphone()));
    mockMvc.perform(get("/api/cellphones"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].imei").value("012345678901237"));
  }

  // ── Códigos de error ───────────────────────────────────────────────────────

  @Test
  @WithMockUser(roles = "ADMIN")
  void shouldReturn409WhenCreatingDuplicateImei() throws Exception {
    when(commandUseCase.create(any()))
        .thenThrow(CellphoneAlreadyExistsException.becauseImeiAlreadyExists("012345678901237"));
    mockMvc.perform(post("/api/cellphones").contentType(MediaType.APPLICATION_JSON)
        .content(validRequest()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void shouldReturn409WhenUpdatingToDuplicateImei() throws Exception {
    when(commandUseCase.update(any()))
        .thenThrow(CellphoneAlreadyExistsException.becauseImeiAlreadyExists("012345678901237"));
    mockMvc.perform(put("/api/cellphones/cell-1").contentType(MediaType.APPLICATION_JSON)
        .content(validRequest())).andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void shouldReturn409WithoutInternalDetailsForOtherDuplicates() throws Exception {
    when(commandUseCase.create(any())).thenThrow(
        new DuplicateKeyException("Duplicate entry 'x' for key 'cellphones.PRIMARY' SQL [INSERT ...]"));
    mockMvc.perform(post("/api/cellphones").contentType(MediaType.APPLICATION_JSON)
        .content(validRequest()))
        .andExpect(status().isConflict())
        .andExpect(content().string(not(containsString("PRIMARY"))))
        .andExpect(content().string(not(containsString("INSERT"))));
  }

  @Test
  @WithMockUser(roles = "REVIEWER")
  void shouldReturn404ForMissingCellphone() throws Exception {
    when(queryUseCase.getById(any())).thenThrow(CellphoneNotFoundException.becauseIdWasNotFound("nope"));
    mockMvc.perform(get("/api/cellphones/nope")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void shouldReturn404WhenUpdatingOrDeletingMissingCellphone() throws Exception {
    when(commandUseCase.update(any())).thenThrow(CellphoneNotFoundException.becauseIdWasNotFound("nope"));
    doThrow(CellphoneNotFoundException.becauseIdWasNotFound("nope")).when(commandUseCase).delete(any());
    mockMvc.perform(put("/api/cellphones/nope").contentType(MediaType.APPLICATION_JSON)
        .content(validRequest())).andExpect(status().isNotFound());
    mockMvc.perform(delete("/api/cellphones/nope")).andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void shouldReturn400ForDomainRuleViolation() throws Exception {
    when(commandUseCase.create(any())).thenThrow(
        InvalidCellphoneException.becauseValueIsInvalid("El IMEI no es válido: el dígito verificador (Luhn) no coincide."));
    mockMvc.perform(post("/api/cellphones").contentType(MediaType.APPLICATION_JSON)
        .content(validRequest().replace("012345678901237", "012345678901234")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(containsString("Luhn")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void shouldReturn400ForMalformedJsonAndWrongTypes() throws Exception {
    mockMvc.perform(post("/api/cellphones").contentType(MediaType.APPLICATION_JSON)
        .content("{\"marca\":")).andExpect(status().isBadRequest());
    mockMvc.perform(post("/api/cellphones").contentType(MediaType.APPLICATION_JSON)
        .content(validRequest().replace("\"ram\":8", "\"ram\":\"ocho\"")))
        .andExpect(status().isBadRequest());
    mockMvc.perform(post("/api/cellphones").contentType(MediaType.APPLICATION_JSON)
        .content(validRequest().replace("\"wifi\":false", "\"wifi\":\"quizas\"")))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(commandUseCase);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void shouldReturn400ForEachMissingOrOutOfRangeField() throws Exception {
    final String[][] invalid = {
      {"\"marca\":\"Acme\"", "\"marca\":\"\""},
      {"\"pulgadas\":6.2", "\"pulgadas\":0"},
      {"\"pulgadas\":6.2", "\"pulgadas\":6.123"},
      {"\"megapixeles\":48", "\"megapixeles\":-1"},
      {"\"ram\":8", "\"ram\":0"},
      {"\"almacenamientoPrincipal\":128", "\"almacenamientoPrincipal\":null"},
      {"\"almacenamientoSecundario\":null", "\"almacenamientoSecundario\":-5"},
      {"\"sistemaOperativo\":\"Android\"", "\"sistemaOperativo\":\"" + "x".repeat(101) + "\""},
      {"\"operador\":\"Carrier\"", "\"operador\":null"},
      {"\"tecnologiaBanda\":\"5G\"", "\"tecnologiaBanda\":\" \""},
      {"\"wifi\":false", "\"wifi\":null"},
      {"\"bluetooth\":false", "\"bluetooth\":null"},
      {"\"camaras\":3", "\"camaras\":0"},
      {"\"marcaCpu\":\"Acme CPU\"", "\"marcaCpu\":\"\""},
      {"\"velocidadCpu\":2.4", "\"velocidadCpu\":0"},
      {"\"nfc\":false", "\"nfc\":null"},
      {"\"huella\":false", "\"huella\":null"},
      {"\"ir\":false", "\"ir\":null"},
      {"\"resisteAgua\":false", "\"resisteAgua\":null"},
      {"\"cantidadSim\":2", "\"cantidadSim\":0"},
      {"\"imei\":\"012345678901237\"", "\"imei\":\"01234567890123A\""}
    };
    for (final String[] change : invalid) {
      final String body = validRequest().replace(change[0], change[1]);
      org.assertj.core.api.Assertions.assertThat(body).as(change[0]).isNotEqualTo(validRequest());
      mockMvc.perform(post("/api/cellphones").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }
    verifyNoInteractions(commandUseCase);
  }

  private static String validRequest() {
    return """
        {"marca":"Acme","imei":"012345678901237","pulgadas":6.2,"megapixeles":48,
        "ram":8,"almacenamientoPrincipal":128,"almacenamientoSecundario":null,
        "sistemaOperativo":"Android","operador":"Carrier","tecnologiaBanda":"5G",
        "wifi":false,"bluetooth":false,"camaras":3,"marcaCpu":"Acme CPU",
        "velocidadCpu":2.4,"nfc":false,"huella":false,"ir":false,
        "resisteAgua":false,"cantidadSim":2}
        """;
  }

  private static CellphoneModel cellphone() {
    return new CellphoneModel("cell-1", "Acme", "012345678901237", BigDecimal.valueOf(6.2),
        BigDecimal.valueOf(48), 8, 128, null, "Android", "Carrier", "5G", false, false,
        3, "Acme CPU", BigDecimal.valueOf(2.4), false, false, false, false, 2);
  }
}
