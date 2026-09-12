package nl.fred.lostandfound.domain.parsing;

import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.domain.exception.InvalidFileException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LostItemTextParser implements LostItemFileParser {

    private static final Pattern FIELD_PATTERN =
            Pattern.compile("^(ItemName|Quantity|Place)\\s*:\\s*(.*)$");

    @Override
    public boolean supports(final MultipartFile file) {
        final String filename = file.getOriginalFilename();
        return "text/plain".equals(file.getContentType())
                || (filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".txt"));
    }

    // PMD CognitiveComplexity/CyclomaticComplexity: this is a state-machine
    // text parser (a loop over lines, tracking a partially-built item across
    // ItemName/Quantity/Place lines) - that shape is inherently a bit
    // branchy. Splitting it further would mean passing the itemName/
    // quantity/place trio in and out of helper methods, which fragments the
    // logic without making any single piece easier to follow.
    // PMD NullAssignment: itemName/quantity/place are reset to null on
    // purpose between blocks - that's how this parser knows "nothing
    // pending" vs. "still building the current item".
    // PMD AssignmentInOperand: `quantity = parseQuantity(value)` inside a
    // switch arrow arm is normal modern switch-expression style, not an
    // accidental assignment-where-a-comparison-was-meant.
    // PMD NonExhaustiveSwitch: the switch's subject is always one of the
    // three literals FIELD_PATTERN's group(1) can capture, so a default
    // arm would be unreachable dead code - see the comment on the switch.
    @SuppressWarnings({
            "PMD.CognitiveComplexity",
            "PMD.CyclomaticComplexity",
            "PMD.NullAssignment",
            "PMD.AssignmentInOperand",
            "PMD.NonExhaustiveSwitch"
    })
    @Override
    public List<LostItem> parse(final String text) {
        final List<LostItem> items = new ArrayList<>();

        String itemName = null;
        Integer quantity = null;
        String place = null;

        for (final String rawLine : text.split("\\R")) {
            final String line = rawLine.trim();

            if (line.isEmpty() || isSeparatorLine(line)) {
                if (itemName != null && quantity != null && place != null) {
                    items.add(toLostItem(itemName, quantity, place));
                }
                itemName = null;
                quantity = null;
                place = null;
                continue;
            }

            final Matcher matcher = FIELD_PATTERN.matcher(line);
            if (!matcher.matches()) {
                continue;
            }

            final String value = matcher.group(2).trim();

            switch (matcher.group(1)) {
                case "ItemName" -> {
                    if (itemName != null && quantity != null && place != null) {
                        items.add(toLostItem(itemName, quantity, place));
                        quantity = null;
                        place = null;
                    }
                    itemName = value;
                }
                case "Quantity" -> quantity = parseQuantity(value);
                case "Place" -> place = value;
                // No default: FIELD_PATTERN only ever captures one of the three
                // literals above into group(1), so a default arm here would be
                // unreachable dead code.
            }
        }

        if (itemName != null && quantity != null && place != null) {
            items.add(toLostItem(itemName, quantity, place));
        }

        return items;
    }

    private LostItem toLostItem(final String itemName, final int quantity, final String place) {
        return LostItem.builder().itemName(itemName).quantity(quantity).place(place).build();
    }

    private boolean isSeparatorLine(final String line) {
        return line.chars().allMatch(c -> c == '-' || c == '_' || c == '=');
    }

    private int parseQuantity(final String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException _) {
            throw new InvalidFileException("Invalid quantity value: '" + value + "'.");
        }
    }

}
