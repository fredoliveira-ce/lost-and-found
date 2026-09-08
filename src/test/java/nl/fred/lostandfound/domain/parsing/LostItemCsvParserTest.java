package nl.fred.lostandfound.domain.parsing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.domain.exception.InvalidFileException;
import org.assertj.core.api.AbstractListAssert;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

@DisplayName("Runs all tests for LostItemCsvParser")
class LostItemCsvParserTest {

  private final LostItemCsvParser parser = new LostItemCsvParser();

  @Test
  @DisplayName("should support .csv files identified by content type or extension, but not .txt")
  void supportsCsvFilesOnly() {
    MockMultipartFile byContentType = new MockMultipartFile("file", "items", "text/csv", new byte[0]);
    MockMultipartFile byExtension = new MockMultipartFile("file", "items.csv", "application/octet-stream", new byte[0]);
    MockMultipartFile txt = new MockMultipartFile("file", "items.txt", "text/plain", new byte[0]);

    assertThat(parser.supports(byContentType)).isTrue();
    assertThat(parser.supports(byExtension)).isTrue();
    assertThat(parser.supports(txt)).isFalse();
  }

  @Test
  @DisplayName("should parse rows using the header to locate each column")
  void parsesRowsByHeader() {
    String csv = """
        ItemName,Quantity,Place
        Laptop,1,Taxi
        Headphones,2,Railway station
        """;

    assertFields(parser.parse(csv))
        .containsExactly(
            tuple("Laptop", 1, "Taxi"),
            tuple("Headphones", 2, "Railway station"));
  }

  @Test
  @DisplayName("should resolve header columns regardless of order or case")
  void resolvesHeaderColumnsRegardlessOfOrder() {
    String csv = """
        place,quantity,itemname
        Airport,4,Jewels
        """;

    assertFields(parser.parse(csv)).containsExactly(tuple("Jewels", 4, "Airport"));
  }

  @Test
  @DisplayName("should honour quoted fields containing commas")
  void honoursQuotedFieldsWithCommas() {
    String csv = """
        ItemName,Quantity,Place
        Jewels,4,"New York, JFK Airport"
        """;

    assertFields(parser.parse(csv)).containsExactly(tuple("Jewels", 4, "New York, JFK Airport"));
  }

  @Test
  @DisplayName("should return an empty list for a file with only a header")
  void returnsEmptyListForHeaderOnlyFile() {
    assertThat(parser.parse("ItemName,Quantity,Place")).isEmpty();
  }

  @Test
  @DisplayName("should return an empty list for blank text")
  void returnsEmptyListForBlankText() {
    assertThat(parser.parse("   \n  ")).isEmpty();
  }

  @Test
  @DisplayName("should throw when a required column is missing from the header")
  void throwsWhenRequiredColumnMissing() {
    String csv = """
        ItemName,Quantity
        Laptop,1
        """;

    assertThatThrownBy(() -> parser.parse(csv))
        .isInstanceOf(InvalidFileException.class);
  }

  @Test
  @DisplayName("should throw when a row has fewer columns than the header")
  void throwsWhenRowHasFewerColumnsThanHeader() {
    String csv = """
        ItemName,Quantity,Place
        Laptop,1
        """;

    assertThatThrownBy(() -> parser.parse(csv))
        .isInstanceOf(InvalidFileException.class);
  }

  @Test
  @DisplayName("should throw when a quantity is not a valid number")
  void throwsForInvalidQuantity() {
    String csv = """
        ItemName,Quantity,Place
        Laptop,not-a-number,Taxi
        """;

    assertThatThrownBy(() -> parser.parse(csv))
        .isInstanceOf(InvalidFileException.class);
  }

  private static AbstractListAssert<?, List<? extends Tuple>, Tuple, ObjectAssert<Tuple>> assertFields(
      List<LostItem> items) {
    return assertThat(items)
        .extracting(LostItem::getItemName, LostItem::getQuantity, LostItem::getPlace);
  }

}
