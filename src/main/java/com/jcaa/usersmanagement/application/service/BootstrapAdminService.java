package com.jcaa.usersmanagement.application.service;

import com.jcaa.usersmanagement.application.port.out.ExistsUserByRolePort;
import com.jcaa.usersmanagement.application.port.out.GetUserByEmailPort;
import com.jcaa.usersmanagement.application.port.out.SaveUserPort;
import com.jcaa.usersmanagement.domain.enums.UserRole;
import com.jcaa.usersmanagement.domain.exception.InvalidUserPasswordException;
import com.jcaa.usersmanagement.domain.exception.UserAlreadyExistsException;
import com.jcaa.usersmanagement.domain.model.UserModel;
import com.jcaa.usersmanagement.domain.valueobject.UserEmail;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import com.jcaa.usersmanagement.domain.valueobject.UserName;
import com.jcaa.usersmanagement.domain.valueobject.UserPassword;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Creates the very first ADMIN of an empty installation. It is a no-op once any ADMIN exists, so
 * leftover bootstrap variables can never mint additional administrators.
 */
@Service
@RequiredArgsConstructor
public class BootstrapAdminService {

  static final int MINIMUM_BOOTSTRAP_PASSWORD_LENGTH = 12;

  private final ExistsUserByRolePort existsUserByRolePort;
  private final GetUserByEmailPort getUserByEmailPort;
  private final SaveUserPort saveUserPort;

  /** @return true when the administrator was created, false when an ADMIN already existed. */
  public boolean createFirstAdminIfNoneExists(
      final String name, final String email, final String plainPassword) {
    if (existsUserByRolePort.existsByRole(UserRole.ADMIN)) {
      return false;
    }
    if (plainPassword == null || plainPassword.trim().length() < MINIMUM_BOOTSTRAP_PASSWORD_LENGTH) {
      throw InvalidUserPasswordException.becauseLengthIsTooShort(MINIMUM_BOOTSTRAP_PASSWORD_LENGTH);
    }
    final UserEmail userEmail = new UserEmail(email);
    getUserByEmailPort
        .getByEmail(userEmail)
        .ifPresent(
            ignored -> {
              throw UserAlreadyExistsException.becauseEmailAlreadyExists(userEmail.value());
            });
    final UserModel admin =
        UserModel.create(
                new UserId(UUID.randomUUID().toString()),
                new UserName(name),
                userEmail,
                UserPassword.fromPlainText(plainPassword),
                UserRole.ADMIN)
            .activate();
    saveUserPort.save(admin);
    return true;
  }
}
