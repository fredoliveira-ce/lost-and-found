package nl.fred.lostandfound.domain.parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.domain.exception.InvalidFileException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LostItemTextParser implements LostItemFileParser {

  private static final Pattern FIELD_PATTERN =
      Pattern.compile("^(ItemName|Quantity|Place)\\s*:\\s*(.*)$");

  @Override
  public boolean supports(MultipartFile file) {
    String filename = file.getOriginalFilename();
    return "text/plain".equals(file.getContentType())
        || (filename != null && filename.toLowerCase().endsWith(".txt"));
  }

  @Override
  public List<LostItem> parse(String text) {
    List<LostItem> items = new ArrayList<>();

    String itemName = null;
    Integer quantity = null;
    String place = null;

    for (String rawLine : text.split("\\R")) {
      String line = rawLine.trim();

      if (line.isEmpty() || isSeparatorLine(line)) {
        if (itemName != null && quantity != null && place != null) {
          items.add(toLostItem(itemName, quantity, place));
        }
        itemName = null;
        quantity = null;
        place = null;
        continue;
      }

      Matcher matcher = FIELD_PATTERN.matcher(line);
      if (!matcher.matches()) {
        continue;
      }

      String value = matcher.group(2).trim();

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
        default -> { }
      }
    }

    if (itemName != null && quantity != null && place != null) {
      items.add(toLostItem(itemName, quantity, place));
    }

    return items;
  }

  private LostItem toLostItem(String itemName, int quantity, String place) {
    return LostItem.builder().itemName(itemName).quantity(quantity).place(place).build();
  }

  private boolean isSeparatorLine(String line) {
    return line.chars().allMatch(c -> c == '-' || c == '_' || c == '=');
  }

  private int parseQuantity(String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException _) {
      throw new InvalidFileException("Invalid quantity value: '" + value + "'.");
    }
  }

}
