package nl.fred.lostandfound.domain.search;

import java.time.Instant;
import java.util.List;
import nl.fred.lostandfound.domain.entity.LostItem;

public record ParsedQuery(String place, Instant from, Instant to, List<String> itemKeywords) {

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
        .noneMatch(keyword -> item.getItemName().toLowerCase().contains(keyword))) {
      return false;
    }
    return true;
  }

}
