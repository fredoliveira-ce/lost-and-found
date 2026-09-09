package nl.fred.lostandfound.domain.exception;

public class InvalidFileException extends ApiException {

  public InvalidFileException(final String message) {
    super(ApiExceptionType.BAD_REQUEST, message);
  }

}
