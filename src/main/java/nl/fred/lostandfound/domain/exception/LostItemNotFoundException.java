package nl.fred.lostandfound.domain.exception;

public class LostItemNotFoundException extends ApiException {

  public LostItemNotFoundException(Long id) {
    super(ApiExceptionType.NOT_FOUND, "Lost item with id = " + id + " was not found.");
  }

}
