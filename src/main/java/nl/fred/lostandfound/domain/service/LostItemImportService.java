package nl.fred.lostandfound.domain.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.fred.lostandfound.data.repository.LostItemRepository;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.domain.exception.InvalidFileException;
import nl.fred.lostandfound.domain.parsing.LostItemFileParser;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class LostItemImportService {

  private final List<LostItemFileParser> parsers;
  private final LostItemRepository repository;

  public List<LostItem> importFrom(final MultipartFile file) {
    if (file.isEmpty()) {
      throw new InvalidFileException("Uploaded file is empty.");
    }

    final LostItemFileParser parser = parsers.stream()
        .filter(candidate -> candidate.supports(file))
        .findFirst()
        .orElseThrow(() -> new InvalidFileException(
            "Unsupported file type: " + file.getContentType()));

    final List<LostItem> lostItems = parser.parse(readAsText(file));

    if (lostItems.isEmpty()) {
      throw new InvalidFileException("No lost items could be extracted from the uploaded file.");
    }

    log.info("Importing {} lost item(s) from file '{}'.", lostItems.size(), file.getOriginalFilename());

    return repository.saveAll(lostItems);
  }

  private String readAsText(final MultipartFile file) {
    try {
      return new String(file.getBytes(), StandardCharsets.UTF_8);
    } catch (IOException _) {
      throw new InvalidFileException("Unable to read uploaded file.");
    }
  }

}
