package com.jcaa.usersmanagement.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jcaa.usersmanagement.application.port.out.ExistsUserByRolePort;
import com.jcaa.usersmanagement.application.port.out.GetUserByEmailPort;
import com.jcaa.usersmanagement.application.port.out.SaveUserPort;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.exception.InvalidUserPasswordException;
import com.jcaa.usersmanagement.domain.exception.UserAlreadyExistsException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BootstrapAdminServiceTest {

  private static final String STRONG_PASSWORD = "Una-clave-larga-2026";

  private ExistsUserByRolePort existsPort;
  private GetUserByEmailPort emailPort;
  private SaveUserPort savePort;
  private BootstrapAdminService service;

  @BeforeEach
  void setUp() {
    existsPort = mock(ExistsUserByRolePort.class);
    emailPort = mock(GetUserByEmailPort.class);
    savePort = mock(SaveUserPort.class);
    service = new BootstrapAdminService(existsPort, emailPort, savePort);
  }

  @Test
  void shouldCreateActiveAdminWithHashedPasswordWhenNoneExists() {
    when(existsPort.existsByRole(UserRole.ADMIN)).thenReturn(false);
    when(emailPort.getByEmail(any())).thenReturn(Optional.empty());

    assertThat(service.createFirstAdminIfNoneExists("Admin", "admin@local.test", STRONG_PASSWORD))
        .isTrue();

    final ArgumentCaptor<UserModel> captor = ArgumentCaptor.forClass(UserModel.class);
    verify(savePort).save(captor.capture());
    final UserModel admin = captor.getValue();
    assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
    assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(admin.getPassword().value()).isNotEqualTo(STRONG_PASSWORD).startsWith("$2");
    assertThat(admin.getPassword().verifyPlain(STRONG_PASSWORD)).isTrue();
  }

  @Test
  void shouldNeverCreateAdditionalAdmins() {
    when(existsPort.existsByRole(UserRole.ADMIN)).thenReturn(true);

    assertThat(service.createFirstAdminIfNoneExists("Other", "other@local.test", STRONG_PASSWORD))
        .isFalse();
    verify(savePort, never()).save(any());
  }

  @Test
  void shouldRejectWeakBootstrapPassword() {
    when(existsPort.existsByRole(UserRole.ADMIN)).thenReturn(false);
    assertThrows(InvalidUserPasswordException.class,
        () -> service.createFirstAdminIfNoneExists("Admin", "admin@local.test", "Short123"));
    assertThrows(InvalidUserPasswordException.class,
        () -> service.createFirstAdminIfNoneExists("Admin", "admin@local.test", null));
    verify(savePort, never()).save(any());
  }

  @Test
  void shouldNotPromoteAnExistingAccountByEmail() {
    when(existsPort.existsByRole(UserRole.ADMIN)).thenReturn(false);
    when(emailPort.getByEmail(any())).thenReturn(Optional.of(mock(UserModel.class)));

    assertThrows(UserAlreadyExistsException.class,
        () -> service.createFirstAdminIfNoneExists("Admin", "member@local.test", STRONG_PASSWORD));
    verify(savePort, never()).save(any());
  }
}
