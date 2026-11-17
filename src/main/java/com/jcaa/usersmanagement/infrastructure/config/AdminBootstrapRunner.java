package com.jcaa.usersmanagement.infrastructure.config;

import com.jcaa.usersmanagement.application.service.BootstrapAdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Opt-in first-administrator bootstrap. Runs only when BOOTSTRAP_ADMIN_EMAIL and
 * BOOTSTRAP_ADMIN_PASSWORD are provided; credentials are never stored in SQL or in the repository.
 */
@Slf4j
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

  private final BootstrapAdminService bootstrapAdminService;
  private final String name;
  private final String email;
  private final String password;

  public AdminBootstrapRunner(
      final BootstrapAdminService bootstrapAdminService,
      @Value("${bootstrap.admin.name:Administrador}") final String name,
      @Value("${bootstrap.admin.email:}") final String email,
      @Value("${bootstrap.admin.password:}") final String password) {
    this.bootstrapAdminService = bootstrapAdminService;
    this.name = name;
    this.email = email;
    this.password = password;
  }

  @Override
  public void run(final ApplicationArguments args) {
    if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
      log.info("[AdminBootstrap] Variables BOOTSTRAP_ADMIN_* no definidas; no se crea administrador.");
      return;
    }
    if (bootstrapAdminService.createFirstAdminIfNoneExists(name, email, password)) {
      log.warn("[AdminBootstrap] Primer ADMIN creado. Elimine BOOTSTRAP_ADMIN_PASSWORD del entorno.");
    } else {
      log.warn("[AdminBootstrap] Ya existe un ADMIN; se ignoran BOOTSTRAP_ADMIN_*. Elimínelas del entorno.");
    }
  }
}
