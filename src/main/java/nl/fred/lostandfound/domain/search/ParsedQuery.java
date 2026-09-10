package nl.fred.lostandfound.domain.search;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import nl.fred.lostandfound.domain.entity.LostItem;

public record ParsedQuery(String place, Instant from, Instant to, List<String> itemKeywords) {

  // SpotBugs EI_EXPOSE_REP/EI_EXPOSE_REP2: defensive copy here means the
  // accessor can safely return the field directly - no one can mutate our
  // internal list, neither through the constructor argument nor the getter.
  public ParsedQuery {
    itemKeywords = List.copyOf(itemKeywords);
  }

  // PMD SimplifyBooleanReturns: it suggests collapsing the last if/return
  // into `return !(...)`, but that would be the only negated return among
  // three early-return guard clauses that otherwise all read the same way
  // - keeping this one consistent with the other two is more readable
  // than the suggested negation.
  @SuppressWarnings("PMD.SimplifyBooleanReturns")
  public boolean matches(final LostItem item) {
    if (place != null && !place.equalsIgnoreCase(item.getPlace())) {
      return false;
    }
    if (from != null && (item.getCreatedAt() == null
        || item.getCreatedAt().isBefore(from)
        || !item.getCreatedAt().isBefore(to))) {
      return false;
    }
    if (!itemKeywords.isEmpty() && itemKeywords.stream()
        .noneMatch(keyword -> item.getItemName().toLowerCase(Locale.ROOT).contains(keyword))) {
      return false;
    }
    return true;
  }

}
