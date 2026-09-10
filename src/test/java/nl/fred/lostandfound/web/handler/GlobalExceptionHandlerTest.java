package nl.fred.lostandfound.web.handler;

import static org.assertj.core.api.Assertions.assertThat;

import nl.fred.lostandfound.domain.exception.ApiException.ApiExceptionType;
import nl.fred.lostandfound.domain.exception.LostItemNotFoundException;
import nl.fred.lostandfound.web.dto.ApiErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@DisplayName("Runs all tests for GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("should map an ApiException to its declared HTTP status and message")
  void handlesApiException() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleApiException(new LostItemNotFoundException(1L));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody().type()).isEqualTo(ApiExceptionType.NOT_FOUND.name());
    assertThat(response.getBody().message()).isEqualTo("Lost item with id = 1 was not found.");
  }

  @Test
  @DisplayName("should map an unexpected exception to 500 without leaking its message")
  void handlesGenericException() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleGenericException(new RuntimeException("sensitive internal detail"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().type()).isEqualTo(ApiExceptionType.INTERNAL_SERVER_ERROR.name());
    assertThat(response.getBody().message()).doesNotContain("sensitive internal detail");
  }

  @Test
  @DisplayName("should map an oversized upload to 400 with a fixed message")
  void handlesMaxUploadSizeExceeded() {
    ResponseEntity<ApiErrorResponse> response =
        handler.handleMaxUploadSizeExceeded(new MaxUploadSizeExceededException(5_000_000L));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().type()).isEqualTo(ApiExceptionType.BAD_REQUEST.name());
    assertThat(response.getBody().message()).isEqualTo("Uploaded file is too large.");
  }

}
