package nl.fred.lostandfound.domain.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.mock.LostItemMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Runs all tests for LostItemSearchEngine")
class LostItemSearchEngineTest {

  private final LostItemSearchEngine engine = new LostItemSearchEngine();

  @Test
  @DisplayName("should match on an exact item name")
  void exactItemNameMatch() {
    LostItem laptop = LostItemMock.getOne("Laptop", "Airport");

    List<LostItem> result = engine.search("laptop", List.of(laptop));

    assertThat(result).containsExactly(laptop);
  }

  @Test
  @DisplayName("should match on an exact place")
  void exactPlaceMatch() {
    LostItem laptop = LostItemMock.getOne("Laptop", "Airport");

    List<LostItem> result = engine.search("airport", List.of(laptop));

    assertThat(result).containsExactly(laptop);
  }

  @Test
  @DisplayName("should tolerate a single-character typo")
  void toleratesTypo() {
    LostItem laptop = LostItemMock.getOne("Laptop", "Airport");

    List<LostItem> result = engine.search("labtop", List.of(laptop));

    assertThat(result).containsExactly(laptop);
  }

  @Test
  @DisplayName("should match a substring of a longer word")
  void matchesSubstring() {
    LostItem badge = LostItemMock.getOne("Badge", "Cafeteria");

    List<LostItem> result = engine.search("cafe", List.of(badge));

    assertThat(result).containsExactly(badge);
  }

  @Test
  @DisplayName("should filter out items with no match")
  void filtersOutNonMatches() {
    LostItem laptop = LostItemMock.getOne("Laptop", "Airport");

    List<LostItem> result = engine.search("umbrella", List.of(laptop));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("should sort matches by score, best match first")
  void sortsByScoreDescending() {
    LostItem exactMatch = LostItemMock.getOne("Wallet", "Cafeteria");
    LostItem fuzzyMatch = LostItemMock.getOne("Wallett", "Airport");

    List<LostItem> result = engine.search("wallet", List.of(fuzzyMatch, exactMatch));

    assertThat(result).containsExactly(exactMatch, fuzzyMatch);
  }

  @Test
  @DisplayName("should not fuzzy-match short query tokens")
  void doesNotFuzzyMatchShortTokens() {
    LostItem bag = LostItemMock.getOne("Bag", "Airport");

    List<LostItem> result = engine.search("bat", List.of(bag));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("should not fuzzy-match against very short item tokens, even a close typo")
  void doesNotFuzzyMatchAgainstShortItemTokens() {
    LostItem keys = LostItemMock.getOne("Key", "Lab");

    List<LostItem> result = engine.search("keys", List.of(keys));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("should return no results for a null query")
  void returnsNoResultsForNullQuery() {
    LostItem laptop = LostItemMock.getOne("Laptop", "Airport");

    List<LostItem> result = engine.search(null, List.of(laptop));

    assertThat(result).isEmpty();
  }

}
