package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jcaa.usersmanagement.application.port.in.CreateUserUseCase;
import com.jcaa.usersmanagement.application.port.in.DeleteUserUseCase;
import com.jcaa.usersmanagement.application.port.in.GetAllUsersUseCase;
import com.jcaa.usersmanagement.application.port.in.GetUserByIdUseCase;
import com.jcaa.usersmanagement.application.port.in.UpdateUserUseCase;
import com.jcaa.usersmanagement.application.service.dto.command.CreateUserCommand;
import com.jcaa.usersmanagement.application.service.dto.command.DeleteUserCommand;
import com.jcaa.usersmanagement.application.service.dto.command.UpdateUserCommand;
import com.jcaa.usersmanagement.application.service.dto.query.GetUserByIdQuery;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import com.jcaa.usersmanagement.application.port.out.GetUserByIdPort;
import com.jcaa.usersmanagement.infrastructure.security.JwtAuthenticationFilter;
import com.jcaa.usersmanagement.infrastructure.security.JwtPrincipal;
import com.jcaa.usersmanagement.infrastructure.security.JwtTokenService;
import com.jcaa.usersmanagement.infrastructure.security.RestAccessDeniedHandler;
import com.jcaa.usersmanagement.infrastructure.security.RestAuthenticationEntryPoint;
import com.jcaa.usersmanagement.infrastructure.security.SecurityConfig;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = UserRestController.class,
    properties = "spring.main.web-application-type=servlet")
@Import({
  SecurityConfig.class,
  JwtAuthenticationFilter.class,
  RestAuthenticationEntryPoint.class,
  RestAccessDeniedHandler.class
})
@DisplayName("UserRestController")
class UserRestControllerTest {

  private static final String ID = "8f684dc4-0c67-4813-8d58-7b5e7f7937b8";

  @Autowired private MockMvc mockMvc;

  @MockBean private CreateUserUseCase createUserUseCase;
  @MockBean private UpdateUserUseCase updateUserUseCase;
  @MockBean private DeleteUserUseCase deleteUserUseCase;
  @MockBean private GetUserByIdUseCase getUserByIdUseCase;
  @MockBean private GetAllUsersUseCase getAllUsersUseCase;
  @MockBean private JwtTokenService jwtTokenService;
  @MockBean private GetUserByIdPort getUserByIdPort;

  @Test
  @DisplayName("GET /api/users debe retornar todos los usuarios")
  void shouldGetAllUsersThroughSpringMvc() throws Exception {
    final UserModel user =
        new UserModel(
            new UserId(ID),
            new UserName("Ada Lovelace"),
            new UserEmail("ada@example.com"),
            UserPassword.fromHash("$2a$12$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu"),
            UserRole.ADMIN,
            UserStatus.ACTIVE);
    when(getAllUsersUseCase.execute()).thenReturn(List.of(user));
    when(jwtTokenService.parse("valid-token")).thenReturn(new JwtPrincipal(ID, UserRole.REVIEWER));
    when(getUserByIdPort.getById(new UserId(ID))).thenReturn(Optional.of(storedUser(UserRole.REVIEWER, UserStatus.ACTIVE)));

    mockMvc
        .perform(get("/api/users").header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(ID))
        .andExpect(jsonPath("$[0].name").value("Ada Lovelace"))
        .andExpect(jsonPath("$[0].email").value("ada@example.com"))
        .andExpect(jsonPath("$[0].role").value("ADMIN"))
        .andExpect(jsonPath("$[0].status").value("ACTIVE"));
  }

  @Test
  @DisplayName("POST /api/users ignora id y role=ADMIN enviados por el cliente y asigna MEMBER")
  void shouldCreateUserThroughSpringMvc() throws Exception {
    final String request =
        """
        {
          "id": "%s",
          "name": "Ada Lovelace",
          "email": "ada@example.com",
          "password": "Secure123!",
          "role": "ADMIN"
        }
        """.formatted(ID);
    final UserModel user =
        new UserModel(
            new UserId(ID),
            new UserName("Ada Lovelace"),
            new UserEmail("ada@example.com"),
            UserPassword.fromHash("$2a$12$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu"),
            UserRole.MEMBER,
            UserStatus.PENDING);
    final ArgumentCaptor<CreateUserCommand> captor =
        ArgumentCaptor.forClass(CreateUserCommand.class);
    when(createUserUseCase.execute(captor.capture())).thenReturn(user);

    mockMvc
        .perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.role").value("MEMBER"));

    final CreateUserCommand command = captor.getValue();
    assertThat(command.role()).isEqualTo("MEMBER");
    assertThat(command.id()).isNotEqualTo(ID);
    assertThat(UUID.fromString(command.id())).isNotNull();
    assertThat(command.email()).isEqualTo("ada@example.com");
  }

  @Test
  @DisplayName("POST /api/users con role=ADMIN nunca crea administradores")
  void shouldNeverForwardAdminRoleFromPublicRegistration() throws Exception {
    final ArgumentCaptor<CreateUserCommand> captor =
        ArgumentCaptor.forClass(CreateUserCommand.class);
    when(createUserUseCase.execute(captor.capture()))
        .thenReturn(
            new UserModel(
                new UserId(ID),
                new UserName("Mallory"),
                new UserEmail("mallory@example.com"),
                UserPassword.fromHash("$2a$12$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu"),
                UserRole.MEMBER,
                UserStatus.PENDING));

    for (final String role : List.of("ADMIN", "admin", "REVIEWER")) {
      mockMvc
          .perform(
              post("/api/users")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {"name":"Mallory","email":"mallory@example.com",
                       "password":"Secure123!","role":"%s","status":"ACTIVE"}
                      """
                          .formatted(role)))
          .andExpect(status().isCreated());
    }
    assertThat(captor.getAllValues()).extracting(CreateUserCommand::role).containsOnly("MEMBER");
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("PUT /api/users/{id} debe actualizar un usuario")
  void shouldUpdateUserThroughSpringMvc() throws Exception {
    final String request =
        """
        {
          "name": "Ada Byron",
          "email": "ada.byron@example.com",
          "password": "NewSecure123!",
          "role": "REVIEWER",
          "status": "ACTIVE"
        }
        """;
    final UserModel user =
        new UserModel(
            new UserId(ID),
            new UserName("Ada Byron"),
            new UserEmail("ada.byron@example.com"),
            UserPassword.fromHash("$2a$12$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu"),
            UserRole.REVIEWER,
            UserStatus.ACTIVE);
    when(updateUserUseCase.execute(
            new UpdateUserCommand(
                ID,
                "Ada Byron",
                "ada.byron@example.com",
                "NewSecure123!",
                "REVIEWER",
                "ACTIVE")))
        .thenReturn(user);

    mockMvc
        .perform(put("/api/users/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Ada Byron"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("DELETE /api/users/{id} debe eliminar un usuario")
  void shouldDeleteUserThroughSpringMvc() throws Exception {
    mockMvc.perform(delete("/api/users/{id}", ID)).andExpect(status().isNoContent());

    verify(deleteUserUseCase).execute(new DeleteUserCommand(ID));
  }

  @Test
  @WithMockUser(roles = "REVIEWER")
  @DisplayName("GET /api/users/{id} debe responder usando el caso de uso inyectado por Spring")
  void shouldGetUserByIdThroughSpringMvc() throws Exception {
    // Arrange
    final UserModel user =
        new UserModel(
            new UserId(ID),
            new UserName("Ada Lovelace"),
            new UserEmail("ada@example.com"),
            UserPassword.fromHash("$2a$12$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu"),
            UserRole.ADMIN,
            UserStatus.ACTIVE);
    when(getUserByIdUseCase.execute(new GetUserByIdQuery(ID))).thenReturn(user);

    // Act & Assert
    mockMvc
        .perform(get("/api/users/{id}", ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ID))
        .andExpect(jsonPath("$.name").value("Ada Lovelace"))
        .andExpect(jsonPath("$.email").value("ada@example.com"))
        .andExpect(jsonPath("$.role").value("ADMIN"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  @DisplayName("POST /api/users debe rechazar una solicitud inválida antes del caso de uso")
  void shouldRejectInvalidCreateRequest() throws Exception {
    // Arrange
    final String invalidRequest =
        """
        {
          "id": "",
          "name": "A",
          "email": "invalid",
          "password": "short",
          "role": ""
        }
        """;

    // Act & Assert
    mockMvc
        .perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(invalidRequest))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  @DisplayName("GET /api/users/{id} debe requerir autenticacion")
  void shouldRejectUnauthenticatedRequest() throws Exception {
    // Arrange

    // Act & Assert
    mockMvc
        .perform(get("/api/users/{id}", ID))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  @WithMockUser(roles = "MEMBER")
  @DisplayName("GET /api/users/{id} debe rechazar un rol sin permisos")
  void shouldRejectUnauthorizedRole() throws Exception {
    // Arrange

    // Act & Assert
    mockMvc
        .perform(get("/api/users/{id}", ID))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  @DisplayName("Un token válido de un usuario desactivado ya no autentica (401)")
  void shouldRejectTokenOfDeactivatedUser() throws Exception {
    when(jwtTokenService.parse("old-token")).thenReturn(new JwtPrincipal(ID, UserRole.ADMIN));
    when(getUserByIdPort.getById(new UserId(ID)))
        .thenReturn(Optional.of(storedUser(UserRole.ADMIN, UserStatus.INACTIVE)));

    mockMvc
        .perform(delete("/api/users/{id}", ID).header("Authorization", "Bearer old-token"))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(deleteUserUseCase);
  }

  @Test
  @DisplayName("Un token de un usuario eliminado ya no autentica (401)")
  void shouldRejectTokenOfDeletedUser() throws Exception {
    when(jwtTokenService.parse("old-token")).thenReturn(new JwtPrincipal(ID, UserRole.ADMIN));
    when(getUserByIdPort.getById(new UserId(ID))).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/users").header("Authorization", "Bearer old-token"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("El rol se toma de la base: un ADMIN degradado a MEMBER recibe 403")
  void shouldUseCurrentRoleInsteadOfTokenClaim() throws Exception {
    when(jwtTokenService.parse("old-token")).thenReturn(new JwtPrincipal(ID, UserRole.ADMIN));
    when(getUserByIdPort.getById(new UserId(ID)))
        .thenReturn(Optional.of(storedUser(UserRole.MEMBER, UserStatus.ACTIVE)));

    mockMvc
        .perform(delete("/api/users/{id}", ID).header("Authorization", "Bearer old-token"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(deleteUserUseCase);
  }

  @Test
  @DisplayName("PUT /api/users/{id} con JSON mal formado responde 400, no 500")
  @WithMockUser(roles = "ADMIN")
  void shouldReturnBadRequestForMalformedJson() throws Exception {
    mockMvc
        .perform(put("/api/users/{id}", ID).contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  private static UserModel storedUser(final UserRole role, final UserStatus status) {
    return new UserModel(
        new UserId(ID),
        new UserName("Ada Lovelace"),
        new UserEmail("ada@example.com"),
        UserPassword.fromHash("$2a$12$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuu"),
        role,
        status);
  }
}
