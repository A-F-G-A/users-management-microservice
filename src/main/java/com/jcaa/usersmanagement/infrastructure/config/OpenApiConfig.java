package com.jcaa.usersmanagement.infrastructure.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configures the authentication mechanism exposed by OpenAPI and Swagger UI. */
@Configuration(proxyBeanMethods = false)
@SecurityScheme(
    name = OpenApiConfig.BEARER_AUTH,
    description = "JWT obtenido en POST /api/auth/login. Ingrese solamente el token.",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
@io.swagger.v3.oas.annotations.OpenAPIDefinition(
    info = @io.swagger.v3.oas.annotations.info.Info(
        title = "Users Management API",
        description = "CRUDL de usuarios y celulares protegidos con JWT según rol."))
public class OpenApiConfig {

  public static final String BEARER_AUTH = "bearerAuth";

  /** Publishes the version from META-INF/build-info.properties (pom.xml) instead of a literal. */
  @Bean
  static OpenApiCustomizer buildVersionOpenApiCustomizer(final ObjectProvider<BuildProperties> build) {
    return openApi -> build.ifAvailable(properties -> openApi.getInfo().setVersion(properties.getVersion()));
  }
}
