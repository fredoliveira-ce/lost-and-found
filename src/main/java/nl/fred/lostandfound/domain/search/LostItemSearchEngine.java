package nl.fred.lostandfound.domain.search;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import nl.fred.lostandfound.domain.entity.LostItem;
import org.springframework.stereotype.Component;

@Component
public class LostItemSearchEngine {

  private static final int EXACT_TOKEN_SCORE = 3;
  private static final int SUBSTRING_SCORE = 2;
  private static final int MIN_FUZZY_QUERY_TOKEN_LENGTH = 4;
  private static final int LONG_ITEM_TOKEN_LENGTH = 7;
  private static final int SHORT_ITEM_TOKEN_LENGTH = 4;

  public List<LostItem> search(final String query, final List<LostItem> candidates) {
    final List<String> queryTokens = tokenize(query);

    return candidates.stream()
        .map(item -> new ScoredLostItem(item, score(queryTokens, item)))
        .filter(scored -> scored.score() > 0)
        .sorted(Comparator.comparingInt(ScoredLostItem::score).reversed())
        .map(ScoredLostItem::item)
        .toList();
  }

  private int score(final List<String> queryTokens, final LostItem item) {
    final String itemText = (item.getItemName() + " " + item.getPlace()).toLowerCase(Locale.ROOT);
    final Set<String> itemTokens = Set.copyOf(tokenize(itemText));

    int score = 0;
    for (final String queryToken : queryTokens) {
      score += tokenScore(queryToken, itemText, itemTokens);
    }
    return score;
  }

  private int tokenScore(final String queryToken, final String itemText, final Set<String> itemTokens) {
    if (itemTokens.contains(queryToken)) {
      return EXACT_TOKEN_SCORE;
    }
    if (itemText.contains(queryToken)) {
      return SUBSTRING_SCORE;
    }
    if (queryToken.length() >= MIN_FUZZY_QUERY_TOKEN_LENGTH) {
      return fuzzyScore(queryToken, itemTokens);
    }
    return 0;
  }

  private int fuzzyScore(final String queryToken, final Set<String> itemTokens) {
    int bestScore = 0;
    for (final String itemToken : itemTokens) {
      final int allowedDistance = allowedDistance(itemToken.length());
      if (allowedDistance == 0) {
        continue;
      }
      final int distance = levenshteinDistance(queryToken, itemToken);
      if (distance <= allowedDistance) {
        bestScore = Math.max(bestScore, allowedDistance - distance + 1);
      }
    }
    return bestScore;
  }

  private int allowedDistance(final int itemTokenLength) {
    if (itemTokenLength >= LONG_ITEM_TOKEN_LENGTH) {
      return 2;
    }
    if (itemTokenLength >= SHORT_ITEM_TOKEN_LENGTH) {
      return 1;
    }
    return 0;
  }

  private List<String> tokenize(final String text) {
    if (text == null) {
      return List.of();
    }
    return List.of(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")).stream()
        .filter(token -> !token.isBlank())
        .collect(Collectors.toList());
  }

  private int levenshteinDistance(final String a, final String b) {
    final int[][] distance = new int[a.length() + 1][b.length() + 1];

    for (int i = 0; i <= a.length(); i++) {
      distance[i][0] = i;
    }
    for (int j = 0; j <= b.length(); j++) {
      distance[0][j] = j;
    }

    for (int i = 1; i <= a.length(); i++) {
      for (int j = 1; j <= b.length(); j++) {
        final int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
        distance[i][j] = Math.min(
            Math.min(distance[i - 1][j] + 1, distance[i][j - 1] + 1),
            distance[i - 1][j - 1] + cost);
      }
    }

    return distance[a.length()][b.length()];
  }

  private record ScoredLostItem(LostItem item, int score) { }

}
