package nl.fred.lostandfound.domain.search;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// PMD TooManyMethods: a direct trade-off against CognitiveComplexity/
// CyclomaticComplexity, which this class used to trip before extractPlace
// was split into findExactPlaceMatch/findFallbackPlaceMatch - more small,
// single-purpose private methods instead of fewer, more complex ones.
// That's the right side of the trade-off here.
@SuppressWarnings("PMD.TooManyMethods")
@Component
public class LostItemQueryParser {

    private static final String TODAY_KEYWORD = "today";
    private static final String YESTERDAY_KEYWORD = "yesterday";

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "lost", "found", "near", "in", "at", "on", "of", "for", "my", "i", "is",
            "was", "are", "were", "that", "with", "around", "by", "from", "item", "items", "last",
            "this", "week", "month", TODAY_KEYWORD, YESTERDAY_KEYWORD);

    private static final int MIN_PLACE_WORD_LENGTH = 4;
    private static final int MIN_KEYWORD_LENGTH = 2;

    private static final String WORD_BOUNDARY = "\\b";

    private static final Pattern LAST_N_DAYS = Pattern.compile("last\\s+(\\d+)\\s+days?");
    private static final Pattern NON_WORD_CHARS = Pattern.compile("[^a-z0-9]+");

    public ParsedQuery parse(final String query, final List<String> knownPlaces) {
        String workingText = query == null ? "" : query.toLowerCase(Locale.ROOT);
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

        if (workingText.contains(YESTERDAY_KEYWORD)) {
            return new DateRange(
                    startOfDay(today.minusDays(1), zone), startOfDay(today, zone),
                    strip(workingText, YESTERDAY_KEYWORD));
        }
        if (workingText.contains(TODAY_KEYWORD)) {
            return new DateRange(
                    startOfDay(today, zone), startOfDay(today.plusDays(1), zone),
                    strip(workingText, TODAY_KEYWORD));
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

    // Split into two passes (PMD flagged the combined method's
    // CognitiveComplexity/CyclomaticComplexity) - each pass is independently
    // simple; extractPlace just tries the first, then falls back to the
    // second.
    private PlaceMatch extractPlace(final String workingText, final List<String> knownPlaces) {
        final PlaceMatch exactMatch = findExactPlaceMatch(workingText, knownPlaces);
        if (exactMatch != null) {
            return exactMatch;
        }

        final PlaceMatch fallbackMatch = findFallbackPlaceMatch(workingText, knownPlaces);
        if (fallbackMatch != null) {
            return fallbackMatch;
        }

        return new PlaceMatch(null, workingText);
    }

    /**
     * First pass: does the working text contain a whole known place name?
     */
    private PlaceMatch findExactPlaceMatch(final String workingText, final List<String> knownPlaces) {
        String bestMatch = null;
        String bestMatchLower = null;

        for (final String place : knownPlaces) {
            final String placeLower = place.toLowerCase(Locale.ROOT);
            if (workingText.contains(placeLower)
                    && (bestMatchLower == null || placeLower.length() > bestMatchLower.length())) {
                bestMatch = place;
                bestMatchLower = placeLower;
            }
        }

        return bestMatch == null ? null : new PlaceMatch(bestMatch, strip(workingText, bestMatchLower));
    }

    /**
     * Fallback pass: does the working text mention a distinctive word from a known place?
     */
    private PlaceMatch findFallbackPlaceMatch(final String workingText, final List<String> knownPlaces) {
        String bestPlace = null;
        String bestWord = null;

        for (final String place : knownPlaces) {
            for (final String word : NON_WORD_CHARS.split(place.toLowerCase(Locale.ROOT))) {
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

        return bestPlace == null ? null : new PlaceMatch(bestPlace, stripWholeWord(workingText, bestWord));
    }

    private List<String> extractKeywords(final String workingText) {
        final List<String> keywords = new ArrayList<>();
        for (final String token : NON_WORD_CHARS.split(workingText)) {
            if (token.length() >= MIN_KEYWORD_LENGTH && !STOPWORDS.contains(token)) {
                keywords.add(token);
            }
        }
        return keywords;
    }

    private boolean containsWholeWord(final String text, final String word) {
        return Pattern.compile(WORD_BOUNDARY + Pattern.quote(word) + WORD_BOUNDARY).matcher(text).find();
    }

    private String stripWholeWord(final String text, final String word) {
        return Pattern.compile(WORD_BOUNDARY + Pattern.quote(word) + WORD_BOUNDARY).matcher(text).replaceFirst(" ");
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
