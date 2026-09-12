package nl.fred.lostandfound.domain.exception;

public class InvalidTokenException extends ApiException {

    public InvalidTokenException() {
        super(ApiExceptionType.UNAUTHORIZED, "Token is missing or has an invalid 'uid' claim.");
    }

}
