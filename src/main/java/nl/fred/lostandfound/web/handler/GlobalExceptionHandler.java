package nl.fred.lostandfound.web.handler;

import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import nl.fred.lostandfound.domain.exception.ApiException;
import nl.fred.lostandfound.domain.exception.ApiException.ApiExceptionType;
import nl.fred.lostandfound.web.dto.ApiErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiErrorResponse> handleApiException(ApiException e) {
    log.warn("Handled API exception [{}]: {}", e.getType(), e.getMessage());

    ApiErrorResponse response = new ApiErrorResponse(e.getType().name(), e.getMessage());

    return ResponseEntity.status(e.getType().getHttpStatus()).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
    String message = e.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + " " + error.getDefaultMessage())
        .collect(Collectors.joining(", "));

    log.warn("Handled validation exception: {}", message);

    ApiErrorResponse response = new ApiErrorResponse(ApiExceptionType.BAD_REQUEST.name(), message);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
    log.warn("Rejected upload exceeding max size: {}", e.getMessage());

    ApiErrorResponse response =
        new ApiErrorResponse(ApiExceptionType.BAD_REQUEST.name(), "Uploaded file is too large.");

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGenericException(Exception e) {
    log.error("Unhandled exception", e);

    ApiErrorResponse response = new ApiErrorResponse(
        ApiExceptionType.INTERNAL_SERVER_ERROR.name(), "An unexpected error occurred.");

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

}
