package nl.fred.lostandfound.domain.parsing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.domain.exception.InvalidFileException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LostItemCsvParser implements LostItemFileParser {

  private static final List<String> REQUIRED_COLUMNS = List.of("itemname", "quantity", "place");

  @Override
  public boolean supports(final MultipartFile file) {
    final String filename = file.getOriginalFilename();
    return "text/csv".equals(file.getContentType())
        || (filename != null && filename.toLowerCase().endsWith(".csv"));
  }

  @Override
  public List<LostItem> parse(final String text) {
    final List<String> lines = text.lines()
        .map(String::trim)
        .filter(line -> !line.isEmpty())
        .toList();

    if (lines.isEmpty()) {
      return List.of();
    }

    final Map<String, Integer> columnIndex = indexColumns(splitCsvLine(lines.get(0)));

    final List<LostItem> items = new ArrayList<>();
    for (final String line : lines.subList(1, lines.size())) {
      final List<String> fields = splitCsvLine(line);

      if (fields.size() < columnIndex.size()) {
        throw new InvalidFileException("CSV row has fewer columns than the header: '" + line + "'.");
      }

      items.add(LostItem.builder()
          .itemName(fields.get(columnIndex.get("itemname")))
          .quantity(parseQuantity(fields.get(columnIndex.get("quantity"))))
          .place(fields.get(columnIndex.get("place")))
          .build());
    }

    return items;
  }

  private Map<String, Integer> indexColumns(final List<String> header) {
    final Map<String, Integer> index = new HashMap<>();
    for (int i = 0; i < header.size(); i++) {
      index.put(header.get(i).toLowerCase(), i);
    }

    for (final String required : REQUIRED_COLUMNS) {
      if (!index.containsKey(required)) {
        throw new InvalidFileException("CSV file is missing required column '" + required + "'.");
      }
    }

    return index;
  }

  private int parseQuantity(final String value) {
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException _) {
      throw new InvalidFileException("Invalid quantity value: '" + value + "'.");
    }
  }

  private List<String> splitCsvLine(final String line) {
    final List<String> fields = new ArrayList<>();
    final StringBuilder current = new StringBuilder();
    boolean inQuotes = false;

    for (int i = 0; i < line.length(); i++) {
      final char c = line.charAt(i);

      if (inQuotes) {
        if (c == '"') {
          if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
            current.append('"');
            i++;
          } else {
            inQuotes = false;
          }
        } else {
          current.append(c);
        }
      } else if (c == '"') {
        inQuotes = true;
      } else if (c == ',') {
        fields.add(current.toString().trim());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }
    fields.add(current.toString().trim());

    return fields;
  }

}
