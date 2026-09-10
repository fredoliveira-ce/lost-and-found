package nl.fred.lostandfound.domain.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.mock.LostItemMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Runs all tests for ParsedQuery")
class ParsedQueryTest {

  private static final Instant NOW = Instant.now();
  private static final Instant FROM = NOW.minus(1, ChronoUnit.DAYS);
  private static final Instant TO = NOW.plus(1, ChronoUnit.DAYS);

  @Test
  @DisplayName("should match an item whose place equals the filter's place, case-insensitively")
  void matchesCaseInsensitivePlace() {
    ParsedQuery query = new ParsedQuery("airport", null, null, List.of());

    assertThat(query.matches(LostItemMock.getOne("Laptop", "Airport"))).isTrue();
  }

  @Test
  @DisplayName("should reject an item whose place differs from the filter's place")
  void rejectsDifferentPlace() {
    ParsedQuery query = new ParsedQuery("Airport", null, null, List.of());

    assertThat(query.matches(LostItemMock.getOne("Laptop", "Taxi"))).isFalse();
  }

  @Test
  @DisplayName("should reject an item with no createdAt when a date range is set")
  void rejectsNullCreatedAtWhenDateRangeSet() {
    ParsedQuery query = new ParsedQuery(null, FROM, TO, List.of());

    assertThat(query.matches(LostItemMock.getOne("Laptop", "Airport"))).isFalse();
  }

  @Test
  @DisplayName("should reject an item created before the date range")
  void rejectsItemCreatedBeforeRange() {
    ParsedQuery query = new ParsedQuery(null, FROM, TO, List.of());
    LostItem tooOld = LostItemMock.getOneWithCreatedAt("Laptop", "Airport", FROM.minus(1, ChronoUnit.DAYS));

    assertThat(query.matches(tooOld)).isFalse();
  }

  @Test
  @DisplayName("should reject an item created at or after the end of the date range")
  void rejectsItemCreatedAtOrAfterRangeEnd() {
    ParsedQuery query = new ParsedQuery(null, FROM, TO, List.of());
    LostItem tooNew = LostItemMock.getOneWithCreatedAt("Laptop", "Airport", TO);

    assertThat(query.matches(tooNew)).isFalse();
  }

  @Test
  @DisplayName("should accept an item created within the date range")
  void acceptsItemWithinRange() {
    ParsedQuery query = new ParsedQuery(null, FROM, TO, List.of());
    LostItem withinRange = LostItemMock.getOneWithCreatedAt("Laptop", "Airport", NOW);

    assertThat(query.matches(withinRange)).isTrue();
  }

  @Test
  @DisplayName("should reject an item whose name matches none of the leftover keywords")
  void rejectsWhenNoKeywordMatches() {
    ParsedQuery query = new ParsedQuery(null, null, null, List.of("umbrella"));

    assertThat(query.matches(LostItemMock.getOne("Laptop", "Airport"))).isFalse();
  }

  @Test
  @DisplayName("should match everything when place, dates, and keywords are all unset")
  void matchesEverythingWhenFilterIsEmpty() {
    ParsedQuery query = new ParsedQuery(null, null, null, List.of());

    assertThat(query.matches(LostItemMock.getOne("Laptop", "Airport"))).isTrue();
  }

}
