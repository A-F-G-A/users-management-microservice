package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.advice;

import com.jcaa.usersmanagement.domain.exception.CellphoneAlreadyExistsException;
import com.jcaa.usersmanagement.domain.exception.CellphoneNotFoundException;
import com.jcaa.usersmanagement.domain.exception.DomainException;
import com.jcaa.usersmanagement.domain.exception.EmailSenderException;
import com.jcaa.usersmanagement.domain.exception.InvalidCredentialsException;
import com.jcaa.usersmanagement.domain.exception.UserAlreadyExistsException;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.infrastructure.adapter.persistence.exception.PersistenceException;
import com.jcaa.usersmanagement.infrastructure.entrypoint.rest.dto.response.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions into stable HTTP codes. 401/403 for the security chain are produced by
 * RestAuthenticationEntryPoint and RestAccessDeniedHandler. Messages never expose SQL, stack
 * traces or class names; unexpected errors are logged server side and answered with 500.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String MALFORMED_REQUEST = "La solicitud está mal formada o contiene tipos inválidos.";
  private static final String DATA_CONFLICT = "La operación entra en conflicto con datos existentes.";
  private static final String INTERNAL_ERROR = "Error interno del servidor.";

  @ExceptionHandler(CellphoneNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiErrorResponse handleCellphoneNotFound(final CellphoneNotFoundException exception) {
    return new ApiErrorResponse(HttpStatus.NOT_FOUND.value(), exception.getMessage());
  }

  @ExceptionHandler(CellphoneAlreadyExistsException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ApiErrorResponse handleCellphoneAlreadyExists(final CellphoneAlreadyExistsException exception) {
    return new ApiErrorResponse(HttpStatus.CONFLICT.value(), exception.getMessage());
  }

  @ExceptionHandler(UserNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiErrorResponse handleUserNotFound(final UserNotFoundException exception) {
    return new ApiErrorResponse(HttpStatus.NOT_FOUND.value(), exception.getMessage());
  }

  @ExceptionHandler(UserAlreadyExistsException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ApiErrorResponse handleUserAlreadyExists(final UserAlreadyExistsException exception) {
    return new ApiErrorResponse(HttpStatus.CONFLICT.value(), exception.getMessage());
  }

  /** Unique-key violations not mapped to a domain conflict (e.g. a race on another key). */
  @ExceptionHandler(DuplicateKeyException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ApiErrorResponse handleDuplicateKey(final DuplicateKeyException exception) {
    log.warn("[GlobalExceptionHandler] Conflicto de unicidad no clasificado.", exception);
    return new ApiErrorResponse(HttpStatus.CONFLICT.value(), DATA_CONFLICT);
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ApiErrorResponse handleInvalidCredentials(final InvalidCredentialsException exception) {
    return new ApiErrorResponse(HttpStatus.UNAUTHORIZED.value(), exception.getMessage());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleConstraintViolation(final ConstraintViolationException exception) {
    final String message =
        exception.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .sorted()
            .collect(Collectors.joining(", "));
    return new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleMethodArgumentNotValid(
      final MethodArgumentNotValidException exception) {
    final String message =
        exception.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .sorted()
            .collect(Collectors.joining(", "));
    return new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), message);
  }

  /** Invalid JSON, wrong JSON types (e.g. "ram":"ocho") or invalid path/query parameter types. */
  @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleMalformedRequest(final Exception ignored) {
    return new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), MALFORMED_REQUEST);
  }

  @ExceptionHandler(EmailSenderException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiErrorResponse handleEmailSender(final EmailSenderException exception) {
    log.error("[GlobalExceptionHandler] Error enviando correo.", exception);
    return new ApiErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), INTERNAL_ERROR);
  }

  /** Remaining domain rule violations (invalid IMEI, role, status, lengths...) are client errors. */
  @ExceptionHandler(DomainException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleDomain(final DomainException exception) {
    return new ApiErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage());
  }

  @ExceptionHandler(PersistenceException.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiErrorResponse handlePersistence(final PersistenceException exception) {
    log.error("[GlobalExceptionHandler] Error de persistencia.", exception);
    return new ApiErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Error de persistencia.");
  }

  /**
   * Spring MVC framework exceptions (405, 404 for unknown routes, 415, missing parameters...)
   * keep their own status; anything else is an unexpected internal error.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGeneral(final Exception exception) {
    if (exception instanceof ErrorResponse errorResponse
        && !errorResponse.getStatusCode().is5xxServerError()) {
      final HttpStatusCode status = errorResponse.getStatusCode();
      final HttpStatus resolved = HttpStatus.resolve(status.value());
      final String message = resolved != null ? resolved.getReasonPhrase() : "Solicitud inválida.";
      return ResponseEntity.status(status).body(new ApiErrorResponse(status.value(), message));
    }
    log.error("[GlobalExceptionHandler] Error no controlado.", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), INTERNAL_ERROR));
  }
}
