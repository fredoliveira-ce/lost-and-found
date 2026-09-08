package nl.fred.lostandfound.domain.exception;

public class InsufficientQuantityException extends ApiException {

  public InsufficientQuantityException(Long lostItemId, int requested, int available) {
    super(
        ApiExceptionType.CONFLICT,
        "Cannot claim " + requested + " item(s) for lost item id = " + lostItemId
            + "; only " + available + " remaining."
    );
  }

}
