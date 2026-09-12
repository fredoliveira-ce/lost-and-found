package nl.fred.lostandfound.domain.exception;

public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(ApiExceptionType.UNAUTHORIZED, "Invalid username or password.");
    }

}
