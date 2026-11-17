package com.jcaa.usersmanagement.domain.exception;

public final class CellphoneAlreadyExistsException extends DomainException {

  private CellphoneAlreadyExistsException(final String imei) {
    super("Ya existe un celular con IMEI '%s'.".formatted(imei));
  }

  public static CellphoneAlreadyExistsException becauseImeiAlreadyExists(final String imei) {
    return new CellphoneAlreadyExistsException(imei);
  }
}
