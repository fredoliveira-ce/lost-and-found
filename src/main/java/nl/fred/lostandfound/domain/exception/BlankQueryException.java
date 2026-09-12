package nl.fred.lostandfound.domain.exception;

public class BlankQueryException extends ApiException {

    public BlankQueryException() {
        super(ApiExceptionType.BAD_REQUEST, "Search query must not be blank.");
    }

}
