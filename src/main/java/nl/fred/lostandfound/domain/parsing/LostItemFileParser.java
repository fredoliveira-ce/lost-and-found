package nl.fred.lostandfound.domain.parsing;

import java.util.List;
import nl.fred.lostandfound.domain.entity.LostItem;
import org.springframework.web.multipart.MultipartFile;

public interface LostItemFileParser {

  boolean supports(MultipartFile file);

  List<LostItem> parse(String text);

}
