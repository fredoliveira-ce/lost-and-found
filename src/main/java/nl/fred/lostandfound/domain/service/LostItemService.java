package nl.fred.lostandfound.domain.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.fred.lostandfound.data.repository.LostItemRepository;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.domain.exception.LostItemNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LostItemService {

  private final LostItemRepository repository;

  public List<LostItem> findAll() {
    return repository.findAll();
  }

  public LostItem findBy(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> {
          log.error("Lost item with id = {} was not found.", id);
          return new LostItemNotFoundException(id);
        });
  }

  public LostItem findByIdForUpdate(Long id) {
    return repository.findByIdForUpdate(id)
        .orElseThrow(() -> {
          log.error("Lost item with id = {} was not found.", id);
          return new LostItemNotFoundException(id);
        });
  }

  public List<LostItem> saveAll(List<LostItem> lostItems) {
    return repository.saveAll(lostItems);
  }

}
