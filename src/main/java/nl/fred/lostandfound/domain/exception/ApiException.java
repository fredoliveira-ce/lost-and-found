package nl.fred.lostandfound.domain.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ApiException extends RuntimeException {

  private final ApiExceptionType type;

  protected ApiException(final ApiExceptionType type, final String message) {
    super(message);
    this.type = type;
  }

  public enum ApiExceptionType {

    NOT_FOUND(HttpStatus.NOT_FOUND),
    BAD_REQUEST(HttpStatus.BAD_REQUEST),
    CONFLICT(HttpStatus.CONFLICT),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    @Getter
    private final HttpStatus httpStatus;

    ApiExceptionType(final HttpStatus httpStatus) {
      this.httpStatus = httpStatus;
    }

  }

}
