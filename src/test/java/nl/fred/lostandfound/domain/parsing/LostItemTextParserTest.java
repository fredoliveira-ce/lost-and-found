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

@DisplayName("Runs all tests for LostItemTextParser")
class LostItemTextParserTest {

  private final LostItemTextParser parser = new LostItemTextParser();

  @Test
  @DisplayName("should support .txt files identified by content type or extension, but not CSV")
  void supportsTextFilesOnly() {
    MockMultipartFile byContentType = new MockMultipartFile("file", "items", "text/plain", new byte[0]);
    MockMultipartFile byExtension = new MockMultipartFile("file", "items.txt", "application/octet-stream", new byte[0]);
    MockMultipartFile csv = new MockMultipartFile("file", "items.csv", "text/csv", new byte[0]);

    assertThat(parser.supports(byContentType)).isTrue();
    assertThat(parser.supports(byExtension)).isTrue();
    assertThat(parser.supports(csv)).isFalse();
  }

  @Test
  @DisplayName("should parse blocks separated by a blank line")
  void parsesBlankLineSeparatedBlocks() {
    String text = """
        ItemName: Laptop
        Quantity: 1
        Place: Taxi

        ItemName: Headphones
        Quantity: 2
        Place: Railway station
        """;

    assertFields(parser.parse(text))
        .containsExactly(
            tuple("Laptop", 1, "Taxi"),
            tuple("Headphones", 2, "Railway station"));
  }

  @Test
  @DisplayName("should parse blocks separated by a dashed rule line")
  void parsesDashSeparatedBlocks() {
    String text = """
        ItemName: Jewels
        Quantity: 4
        Place: Airport
        --------------------------
        ItemName: Laptop
        Quantity: 1
        Place: Airport
        """;

    assertFields(parser.parse(text))
        .containsExactly(
            tuple("Jewels", 4, "Airport"),
            tuple("Laptop", 1, "Airport"));
  }

  @Test
  @DisplayName("should start a new record when ItemName repeats without a separator")
  void startsNewRecordWithoutSeparator() {
    String text = """
        ItemName: Jewels
        Quantity: 4
        Place: Airport
        ItemName: Laptop
        Quantity: 1
        Place: Airport
        """;

    assertFields(parser.parse(text))
        .containsExactly(
            tuple("Jewels", 4, "Airport"),
            tuple("Laptop", 1, "Airport"));
  }

  @Test
  @DisplayName("should ignore unrelated lines")
  void ignoresUnrelatedLines() {
    String text = """
        Lost & Found report

        ItemName: Laptop
        Quantity: 1
        Place: Taxi
        """;

    assertFields(parser.parse(text)).containsExactly(tuple("Laptop", 1, "Taxi"));
  }

  @Test
  @DisplayName("should return an empty list for text with no recognizable records")
  void returnsEmptyListForUnrecognizedText() {
    assertThat(parser.parse("nothing to see here")).isEmpty();
  }

  @Test
  @DisplayName("should discard an incomplete trailing record")
  void discardsIncompleteTrailingRecord() {
    String text = """
        ItemName: Laptop
        Quantity: 1
        Place: Taxi

        ItemName: Headphones
        Quantity: 2
        """;

    assertFields(parser.parse(text)).containsExactly(tuple("Laptop", 1, "Taxi"));
  }

  @Test
  @DisplayName("should throw when a quantity is not a valid number")
  void throwsForInvalidQuantity() {
    String text = """
        ItemName: Laptop
        Quantity: not-a-number
        Place: Taxi
        """;

    assertThatThrownBy(() -> parser.parse(text))
        .isInstanceOf(InvalidFileException.class);
  }

  // LostItem's equality is id-only (see Claim/LostItem's @EqualsAndHashCode),
  // so freshly-parsed items (no id yet) would all compare equal to each
  // other; assert on the actual fields instead.
  private static AbstractListAssert<?, List<? extends Tuple>, Tuple, ObjectAssert<Tuple>> assertFields(
      List<LostItem> items) {
    return assertThat(items)
        .extracting(LostItem::getItemName, LostItem::getQuantity, LostItem::getPlace);
  }

}
