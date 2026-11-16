package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jcaa.usersmanagement.application.port.in.CellphoneCommandUseCase;
import com.jcaa.usersmanagement.application.port.in.CellphoneQueryUseCase;
import com.jcaa.usersmanagement.domain.model.CellphoneModel;
import com.jcaa.usersmanagement.infrastructure.config.OpenApiConfig;
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
        .andExpect(jsonPath("$.imei").value("012345678901234"))
        .andExpect(jsonPath("$.wifi").value(false));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void shouldRejectInvalidImei() throws Exception {
    mockMvc.perform(post("/api/cellphones").contentType(MediaType.APPLICATION_JSON)
        .content(validRequest().replace("012345678901234", "123")))
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

  private static String validRequest() {
    return """
        {"marca":"Acme","imei":"012345678901234","pulgadas":6.2,"megapixeles":48,
        "ram":8,"almacenamientoPrincipal":128,"almacenamientoSecundario":null,
        "sistemaOperativo":"Android","operador":"Carrier","tecnologiaBanda":"5G",
        "wifi":false,"bluetooth":false,"camaras":3,"marcaCpu":"Acme CPU",
        "velocidadCpu":2.4,"nfc":false,"huella":false,"ir":false,
        "resisteAgua":false,"cantidadSim":2}
        """;
  }

  private static CellphoneModel cellphone() {
    return new CellphoneModel("cell-1", "Acme", "012345678901234", BigDecimal.valueOf(6.2),
        BigDecimal.valueOf(48), 8, 128, null, "Android", "Carrier", "5G", false, false,
        3, "Acme CPU", BigDecimal.valueOf(2.4), false, false, false, false, 2);
  }
}
