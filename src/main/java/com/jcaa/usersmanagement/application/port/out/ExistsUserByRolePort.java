package com.jcaa.usersmanagement.application.port.out;

import com.jcaa.usersmanagement.domain.enums.UserRole;

public interface ExistsUserByRolePort {
  boolean existsByRole(UserRole role);
}
