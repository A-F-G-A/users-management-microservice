package com.jcaa.usersmanagement.domain.exception;

public final class InvalidCellphoneException extends DomainException {

  private InvalidCellphoneException(final String message) {
    super(message);
  }

  public static InvalidCellphoneException becauseValueIsInvalid(final String message) {
    return new InvalidCellphoneException(message);
  }
}
