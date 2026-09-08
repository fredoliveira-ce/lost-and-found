package nl.fred.lostandfound.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import nl.fred.lostandfound.data.repository.LostItemRepository;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.domain.exception.InvalidFileException;
import nl.fred.lostandfound.domain.parsing.LostItemFileParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

@DisplayName("Runs all tests for LostItemImportService")
class LostItemImportServiceTest {

  private LostItemImportService service;
  private LostItemFileParser parser;
  private LostItemRepository repository;

  @BeforeEach
  void beforeEach() {
    parser = Mockito.mock(LostItemFileParser.class);
    repository = Mockito.mock(LostItemRepository.class);
    service = new LostItemImportService(List.of(parser), repository);
  }

  @Test
  @DisplayName("should parse and save the lost items from a supported file")
  void importsSupportedFile() {
    MockMultipartFile file = new MockMultipartFile("file", "items.txt", "text/plain", "content".getBytes());
    LostItem parsedItem = LostItem.builder().itemName("Laptop").quantity(1).place("Taxi").build();

    Mockito.when(parser.supports(file)).thenReturn(true);
    Mockito.when(parser.parse("content")).thenReturn(List.of(parsedItem));
    Mockito.when(repository.saveAll(Mockito.anyList()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    List<LostItem> result = service.importFrom(file);

    assertThat(result).containsExactly(parsedItem);

    ArgumentCaptor<List<LostItem>> captor = ArgumentCaptor.forClass(List.class);
    Mockito.verify(repository).saveAll(captor.capture());
    assertThat(captor.getValue()).containsExactly(parsedItem);
  }

  @Test
  @DisplayName("should reject an empty file")
  void rejectsEmptyFile() {
    MockMultipartFile file = new MockMultipartFile("file", "items.txt", "text/plain", new byte[0]);

    assertThatThrownBy(() -> service.importFrom(file))
        .isInstanceOf(InvalidFileException.class);

    Mockito.verifyNoInteractions(repository);
  }

  @Test
  @DisplayName("should reject a file type with no supporting parser")
  void rejectsUnsupportedFileType() {
    MockMultipartFile file = new MockMultipartFile("file", "items.exe", "application/octet-stream", "content".getBytes());

    Mockito.when(parser.supports(file)).thenReturn(false);

    assertThatThrownBy(() -> service.importFrom(file))
        .isInstanceOf(InvalidFileException.class);

    Mockito.verifyNoInteractions(repository);
  }

  @Test
  @DisplayName("should reject a file from which no lost items could be parsed")
  void rejectsFileWithNoParsableItems() {
    MockMultipartFile file = new MockMultipartFile("file", "items.txt", "text/plain", "content".getBytes());

    Mockito.when(parser.supports(file)).thenReturn(true);
    Mockito.when(parser.parse("content")).thenReturn(List.of());

    assertThatThrownBy(() -> service.importFrom(file))
        .isInstanceOf(InvalidFileException.class);

    Mockito.verifyNoInteractions(repository);
  }

}
