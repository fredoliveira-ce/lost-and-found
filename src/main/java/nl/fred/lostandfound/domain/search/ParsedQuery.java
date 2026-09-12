package nl.fred.lostandfound.domain.search;

import nl.fred.lostandfound.domain.entity.LostItem;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

public record ParsedQuery(String place, Instant from, Instant to, List<String> itemKeywords) {

    // SpotBugs EI_EXPOSE_REP/EI_EXPOSE_REP2: defensive copy here means the
    // accessor can safely return the field directly - no one can mutate our
    // internal list, neither through the constructor argument nor the getter.
    public ParsedQuery {
        itemKeywords = List.copyOf(itemKeywords);
    }

    public boolean matches(final LostItem item) {
        if (place != null && !place.equalsIgnoreCase(item.getPlace())) {
            return false;
        }
        if (from != null && (item.getCreatedAt() == null
                || item.getCreatedAt().isBefore(from)
                || !item.getCreatedAt().isBefore(to))) {
            return false;
        }
        return itemKeywords.isEmpty() || itemKeywords.stream()
                .anyMatch(keyword -> item.getItemName().toLowerCase(Locale.ROOT).contains(keyword));
    }

}
