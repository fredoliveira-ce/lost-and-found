package nl.fred.lostandfound.domain.exception;

public class InsufficientQuantityException extends ApiException {

    public InsufficientQuantityException(final Long lostItemId, final int requested, final int available) {
        super(
            ApiExceptionType.CONFLICT,
            "Cannot claim " + requested + " item(s) for lost item id = " + lostItemId
                    + "; only " + available + " remaining."
        );
    }

}
