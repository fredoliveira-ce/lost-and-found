package nl.fred.lostandfound.domain.search;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class LostItemQueryParser {

  private static final Set<String> STOPWORDS = Set.of(
      "a", "an", "the", "lost", "found", "near", "in", "at", "on", "of", "for", "my", "i", "is",
      "was", "are", "were", "that", "with", "around", "by", "from", "item", "items", "last",
      "this", "week", "month", "today", "yesterday");

  private static final int MIN_PLACE_WORD_LENGTH = 4;
  private static final int MIN_KEYWORD_LENGTH = 2;

  private static final Pattern LAST_N_DAYS = Pattern.compile("last\\s+(\\d+)\\s+days?");

  public ParsedQuery parse(final String query, final List<String> knownPlaces) {
    String workingText = query == null ? "" : query.toLowerCase();
    final ZoneId zone = ZoneId.systemDefault();

    final DateRange dateRange = extractDateRange(workingText, zone);
    workingText = dateRange.remainingText();

    final PlaceMatch placeMatch = extractPlace(workingText, knownPlaces);
    workingText = placeMatch.remainingText();

    final List<String> itemKeywords = extractKeywords(workingText);

    return new ParsedQuery(placeMatch.place(), dateRange.from(), dateRange.to(), itemKeywords);
  }

  private DateRange extractDateRange(final String workingText, final ZoneId zone) {
    final LocalDate today = LocalDate.now(zone);

    final Matcher lastNDays = LAST_N_DAYS.matcher(workingText);
    if (lastNDays.find()) {
      final long days = Long.parseLong(lastNDays.group(1));
      final Instant to = Instant.now();
      final Instant from = to.minus(days, ChronoUnit.DAYS);
      return new DateRange(from, to, strip(workingText, lastNDays.group()));
    }

    if (workingText.contains("yesterday")) {
      return new DateRange(
          startOfDay(today.minusDays(1), zone), startOfDay(today, zone),
          strip(workingText, "yesterday"));
    }
    if (workingText.contains("today")) {
      return new DateRange(
          startOfDay(today, zone), startOfDay(today.plusDays(1), zone),
          strip(workingText, "today"));
    }
    if (workingText.contains("last week")) {
      final Instant startOfThisWeek = startOfWeek(today, zone);
      return new DateRange(
          startOfThisWeek.minus(7, ChronoUnit.DAYS), startOfThisWeek,
          strip(workingText, "last week"));
    }
    if (workingText.contains("this week")) {
      return new DateRange(
          startOfWeek(today, zone), startOfDay(today.plusDays(1), zone),
          strip(workingText, "this week"));
    }
    if (workingText.contains("last month")) {
      final LocalDate firstOfThisMonth = today.withDayOfMonth(1);
      return new DateRange(
          startOfDay(firstOfThisMonth.minusMonths(1), zone), startOfDay(firstOfThisMonth, zone),
          strip(workingText, "last month"));
    }
    if (workingText.contains("this month")) {
      final LocalDate firstOfThisMonth = today.withDayOfMonth(1);
      return new DateRange(
          startOfDay(firstOfThisMonth, zone), startOfDay(today.plusDays(1), zone),
          strip(workingText, "this month"));
    }

    return new DateRange(null, null, workingText);
  }

  private PlaceMatch extractPlace(final String workingText, final List<String> knownPlaces) {
    String bestMatch = null;
    String bestMatchLower = null;

    for (final String place : knownPlaces) {
      final String placeLower = place.toLowerCase();
      if (workingText.contains(placeLower)
          && (bestMatchLower == null || placeLower.length() > bestMatchLower.length())) {
        bestMatch = place;
        bestMatchLower = placeLower;
      }
    }

    if (bestMatch != null) {
      return new PlaceMatch(bestMatch, strip(workingText, bestMatchLower));
    }

    String bestPlace = null;
    String bestWord = null;

    for (final String place : knownPlaces) {
      for (final String word : place.toLowerCase().split("[^a-z0-9]+")) {
        if (word.length() < MIN_PLACE_WORD_LENGTH || STOPWORDS.contains(word)) {
          continue;
        }
        if (containsWholeWord(workingText, word)
            && (bestWord == null || word.length() > bestWord.length())) {
          bestPlace = place;
          bestWord = word;
        }
      }
    }

    if (bestPlace != null) {
      return new PlaceMatch(bestPlace, stripWholeWord(workingText, bestWord));
    }

    return new PlaceMatch(null, workingText);
  }

  private List<String> extractKeywords(final String workingText) {
    final List<String> keywords = new ArrayList<>();
    for (final String token : workingText.split("[^a-z0-9]+")) {
      if (token.length() >= MIN_KEYWORD_LENGTH && !STOPWORDS.contains(token)) {
        keywords.add(token);
      }
    }
    return keywords;
  }

  private boolean containsWholeWord(final String text, final String word) {
    return Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(text).find();
  }

  private String stripWholeWord(final String text, final String word) {
    return Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(text).replaceFirst(" ");
  }

  private String strip(final String text, final String match) {
    return text.replaceFirst(Pattern.quote(match), " ");
  }

  private Instant startOfDay(final LocalDate date, final ZoneId zone) {
    return date.atStartOfDay(zone).toInstant();
  }

  private Instant startOfWeek(final LocalDate date, final ZoneId zone) {
    return startOfDay(date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), zone);
  }

  private record DateRange(Instant from, Instant to, String remainingText) { }

  private record PlaceMatch(String place, String remainingText) { }

}
