package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Public self-registration payload. It deliberately has no id or role: the server generates the
 * identifier and always assigns MEMBER, so unknown fields such as "role":"ADMIN" are ignored.
 */
public record CreateUserRestRequest(
    @NotBlank(message = "name must not be blank")
        @Size(min = 3, max = 100, message = "name must have between 3 and 100 characters")
        String name,
    @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        @Size(max = 150, message = "email must have at most 150 characters")
        String email,
    @NotBlank(message = "password must not be blank")
        @Size(min = 8, max = 64, message = "password must have between 8 and 64 characters")
        String password) {}
