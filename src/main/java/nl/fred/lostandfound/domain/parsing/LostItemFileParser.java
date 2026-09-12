package nl.fred.lostandfound.domain.parsing;

import nl.fred.lostandfound.domain.entity.LostItem;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface LostItemFileParser {

    boolean supports(MultipartFile file);

    List<LostItem> parse(String text);

}
