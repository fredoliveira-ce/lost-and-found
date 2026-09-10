package nl.fred.lostandfound.domain.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import nl.fred.lostandfound.mock.LostItemMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Runs all tests for LostItemQueryParser")
class LostItemQueryParserTest {

  private static final List<String> KNOWN_PLACES = List.of("Airport", "Cafeteria");

  private final LostItemQueryParser parser = new LostItemQueryParser();

  @Test
  @DisplayName("should recognize a known place, preserving its original casing")
  void recognizesKnownPlace() {
    ParsedQuery result = parser.parse("something near the cafeteria", KNOWN_PLACES);

    assertThat(result.place()).isEqualTo("Cafeteria");
  }

  @Test
  @DisplayName("should ignore a place-like word that isn't in the known list")
  void ignoresUnknownPlace() {
    ParsedQuery result = parser.parse("left at the gym", KNOWN_PLACES);

    assertThat(result.place()).isNull();
  }

  @Test
  @DisplayName("should fall back to a distinctive word when the full place name isn't mentioned")
  void fallsBackToDistinctiveWordOfMultiWordPlace() {
    ParsedQuery result = parser.parse(
        "left near the library", List.of("Central Library", "Cafeteria"));

    assertThat(result.place()).isEqualTo("Central Library");
  }

  @Test
  @DisplayName("should prefer the longer/more specific exact match when multiple places match")
  void prefersLongerExactMatchOverShorterOne() {
    ParsedQuery result = parser.parse(
        "left at the airport terminal", List.of("Airport", "Airport Terminal"));

    assertThat(result.place()).isEqualTo("Airport Terminal");
  }

  @Test
  @DisplayName("should keep the first exact match when a later, shorter match isn't more specific")
  void keepsExactMatchOverLessSpecificOne() {
    ParsedQuery result = parser.parse(
        "left at the airport terminal", List.of("Airport Terminal", "Airport"));

    assertThat(result.place()).isEqualTo("Airport Terminal");
  }

  @Test
  @DisplayName("should skip a place word that is also a stopword when falling back")
  void fallbackSkipsStopwordPlaceWord() {
    ParsedQuery result = parser.parse(
        "left it with someone", List.of("With Annex"));

    assertThat(result.place()).isNull();
  }

  @Test
  @DisplayName("should prefer the longer of two distinctive fallback words that both match")
  void fallbackPrefersLongerOfTwoMatchingWords() {
    ParsedQuery result = parser.parse(
        "seen near the library annex",
        List.of("Central Annex", "Central Library"));

    assertThat(result.place()).isEqualTo("Central Library");
  }

  @Test
  @DisplayName("should treat a null query the same as an empty one")
  void treatsNullQueryAsEmpty() {
    ParsedQuery result = parser.parse(null, KNOWN_PLACES);

    assertThat(result.place()).isNull();
    assertThat(result.from()).isNull();
    assertThat(result.itemKeywords()).isEmpty();
  }

  @Test
  @DisplayName("should recognize 'today'")
  void recognizesToday() {
    ParsedQuery result = parser.parse("lost today", KNOWN_PLACES);

    assertRangeIncludes(result, Instant.now());
    assertRangeExcludes(result, Instant.now().minus(2, ChronoUnit.DAYS));
  }

  @Test
  @DisplayName("should recognize 'yesterday'")
  void recognizesYesterday() {
    ParsedQuery result = parser.parse("lost yesterday", KNOWN_PLACES);

    assertThat(result.from()).isNotNull();
    assertThat(result.to()).isNotNull();
    assertRangeExcludes(result, Instant.now());
  }

  @Test
  @DisplayName("should recognize 'this week'")
  void recognizesThisWeek() {
    ParsedQuery result = parser.parse("lost this week", KNOWN_PLACES);

    assertRangeIncludes(result, Instant.now());
    assertRangeExcludes(result, Instant.now().minus(30, ChronoUnit.DAYS));
  }

  @Test
  @DisplayName("should recognize 'last week'")
  void recognizesLastWeek() {
    ParsedQuery result = parser.parse("lost last week", KNOWN_PLACES);

    assertRangeExcludes(result, Instant.now());
  }

  @Test
  @DisplayName("should recognize 'this month'")
  void recognizesThisMonth() {
    ParsedQuery result = parser.parse("lost this month", KNOWN_PLACES);

    assertRangeIncludes(result, Instant.now());
  }

  @Test
  @DisplayName("should recognize 'last month'")
  void recognizesLastMonth() {
    ParsedQuery result = parser.parse("lost last month", KNOWN_PLACES);

    assertRangeExcludes(result, Instant.now());
  }

  @Test
  @DisplayName("should recognize 'last N days'")
  void recognizesLastNDays() {
    ParsedQuery result = parser.parse("lost last 3 days", KNOWN_PLACES);

    assertRangeIncludes(result, Instant.now().minus(1, ChronoUnit.DAYS));
    assertRangeExcludes(result, Instant.now().minus(5, ChronoUnit.DAYS));
  }

  @Test
  @DisplayName("should extract item keywords when no place or date is recognized")
  void extractsKeywordsOnly() {
    ParsedQuery result = parser.parse("black wallet", KNOWN_PLACES);

    assertThat(result.place()).isNull();
    assertThat(result.from()).isNull();
    assertThat(result.to()).isNull();
    assertThat(result.itemKeywords()).contains("black", "wallet");
  }

  @Test
  @DisplayName("should extract place, date and keyword together")
  void extractsAllDimensions() {
    ParsedQuery result = parser.parse("black bag lost near the cafeteria last week", KNOWN_PLACES);

    assertThat(result.place()).isEqualTo("Cafeteria");
    assertThat(result.from()).isNotNull();
    assertThat(result.to()).isNotNull();
    assertThat(result.itemKeywords()).contains("black", "bag");
  }

  @Test
  @DisplayName("should produce an empty filter when nothing recognizable is left")
  void garbageInputProducesEmptyFilter() {
    ParsedQuery result = parser.parse("the a of", KNOWN_PLACES);

    assertThat(result.place()).isNull();
    assertThat(result.from()).isNull();
    assertThat(result.to()).isNull();
    assertThat(result.itemKeywords()).isEmpty();
  }

  @Test
  @DisplayName("matches() should use OR semantics across keywords, ignoring unmodeled attributes")
  void matchesUsesOrSemanticsAcrossKeywords() {
    ParsedQuery result = new ParsedQuery(null, null, null, List.of("black", "bag"));

    assertThat(result.matches(LostItemMock.getOne("Bag", "Airport"))).isTrue();
  }

  private void assertRangeIncludes(ParsedQuery result, Instant instant) {
    assertThat(result.from()).isNotNull();
    assertThat(result.to()).isNotNull();
    assertThat(instant).isAfterOrEqualTo(result.from()).isBefore(result.to());
  }

  private void assertRangeExcludes(ParsedQuery result, Instant instant) {
    assertThat(result.from()).isNotNull();
    assertThat(result.to()).isNotNull();
    boolean withinRange = !instant.isBefore(result.from()) && instant.isBefore(result.to());
    assertThat(withinRange).isFalse();
  }

}
