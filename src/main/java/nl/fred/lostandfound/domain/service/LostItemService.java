package nl.fred.lostandfound.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.fred.lostandfound.data.repository.LostItemRepository;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.domain.exception.BlankQueryException;
import nl.fred.lostandfound.domain.exception.LostItemNotFoundException;
import nl.fred.lostandfound.domain.search.LostItemQueryParser;
import nl.fred.lostandfound.domain.search.LostItemSearchEngine;
import nl.fred.lostandfound.domain.search.ParsedQuery;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LostItemService {

    private final LostItemRepository repository;
    private final LostItemSearchEngine searchEngine;
    private final LostItemQueryParser queryParser;

    public List<LostItem> findAll() {
        return repository.findAll();
    }

    public List<LostItem> search(final String q) {
        requireNonBlank(q);
        return searchEngine.search(q, repository.findAll());
    }

    public List<LostItem> query(final String q) {
        requireNonBlank(q);
        final ParsedQuery parsedQuery = queryParser.parse(q, repository.findDistinctPlaces());
        return repository.findAll().stream().filter(parsedQuery::matches).toList();
    }

    private void requireNonBlank(final String q) {
        if (q == null || q.isBlank()) {
            throw new BlankQueryException();
        }
    }

    public LostItem findBy(final Long id) {
        return repository.findById(id)
                .orElseThrow(() -> {
                    log.error("Lost item with id = {} was not found.", id);
                    return new LostItemNotFoundException(id);
                });
    }

    public LostItem findByIdForUpdate(final Long id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> {
                    log.error("Lost item with id = {} was not found.", id);
                    return new LostItemNotFoundException(id);
                });
    }

    public List<LostItem> saveAll(final List<LostItem> lostItems) {
        return repository.saveAll(lostItems);
    }

}
