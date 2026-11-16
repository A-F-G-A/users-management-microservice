package com.jcaa.usersmanagement.domain.exception;

public final class CellphoneNotFoundException extends DomainException {

  private CellphoneNotFoundException(final String message) {
    super(message);
  }

  public static CellphoneNotFoundException becauseIdWasNotFound(final String id) {
    return new CellphoneNotFoundException("Celular con id '%s' no encontrado.".formatted(id));
  }
}
