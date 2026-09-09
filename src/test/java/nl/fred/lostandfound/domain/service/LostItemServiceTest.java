package nl.fred.lostandfound.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import nl.fred.lostandfound.data.repository.LostItemRepository;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.domain.exception.BlankQueryException;
import nl.fred.lostandfound.domain.exception.LostItemNotFoundException;
import nl.fred.lostandfound.domain.search.LostItemQueryParser;
import nl.fred.lostandfound.domain.search.LostItemSearchEngine;
import nl.fred.lostandfound.domain.search.ParsedQuery;
import nl.fred.lostandfound.mock.LostItemMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("Runs all tests for LostItemService")
class LostItemServiceTest {

  private LostItemService service;
  private LostItemRepository repository;
  private LostItemSearchEngine searchEngine;
  private LostItemQueryParser queryParser;

  @BeforeEach
  void beforeEach() {
    repository = Mockito.mock(LostItemRepository.class);
    searchEngine = Mockito.mock(LostItemSearchEngine.class);
    queryParser = Mockito.mock(LostItemQueryParser.class);
    service = new LostItemService(repository, searchEngine, queryParser);
  }

  @Test
  @DisplayName("should find all lost items")
  void findAllDelegatesToRepository() {
    LostItem lostItem = LostItemMock.getOne();
    Mockito.when(repository.findAll()).thenReturn(List.of(lostItem));

    List<LostItem> result = service.findAll();

    assertThat(result).containsExactly(lostItem);
  }

  @Test
  @DisplayName("should find a lost item by id")
  void findByIdSuccessfully() {
    LostItem lostItem = LostItemMock.getOne();
    Mockito.when(repository.findById(lostItem.getId())).thenReturn(Optional.of(lostItem));

    LostItem result = service.findBy(lostItem.getId());

    assertThat(result).isEqualTo(lostItem);
  }

  @Test
  @DisplayName("should throw when the lost item does not exist")
  void findByIdThrowsWhenNotFound() {
    Mockito.when(repository.findById(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.findBy(1L))
        .isInstanceOf(LostItemNotFoundException.class);
  }

  @Test
  @DisplayName("should save all lost items")
  void saveAllDelegatesToRepository() {
    List<LostItem> lostItems = List.of(LostItemMock.getOne());
    Mockito.when(repository.saveAll(lostItems)).thenReturn(lostItems);

    List<LostItem> result = service.saveAll(lostItems);

    assertThat(result).isEqualTo(lostItems);
    Mockito.verify(repository).saveAll(lostItems);
  }

  @Test
  @DisplayName("should throw when searching with a blank query")
  void searchThrowsForBlankQuery() {
    assertThatThrownBy(() -> service.search(" "))
        .isInstanceOf(BlankQueryException.class);

    Mockito.verifyNoInteractions(searchEngine);
  }

  @Test
  @DisplayName("should delegate search to the search engine")
  void searchDelegatesToSearchEngine() {
    LostItem lostItem = LostItemMock.getOne();
    List<LostItem> allItems = List.of(lostItem);
    Mockito.when(repository.findAll()).thenReturn(allItems);
    Mockito.when(searchEngine.search("laptop", allItems)).thenReturn(allItems);

    List<LostItem> result = service.search("laptop");

    assertThat(result).isEqualTo(allItems);
  }

  @Test
  @DisplayName("should throw when running a query with a blank query")
  void queryThrowsForBlankQuery() {
    assertThatThrownBy(() -> service.query(null))
        .isInstanceOf(BlankQueryException.class);

    Mockito.verifyNoInteractions(queryParser);
  }

  @Test
  @DisplayName("should filter lost items using the parsed query")
  void queryFiltersUsingParsedQuery() {
    LostItem matching = LostItemMock.getOne("Wallet", "Airport");
    LostItem nonMatching = LostItemMock.getOne("Bag", "Cafeteria");
    List<LostItem> allItems = List.of(matching, nonMatching);
    List<String> knownPlaces = List.of("Airport", "Cafeteria");

    Mockito.when(repository.findDistinctPlaces()).thenReturn(knownPlaces);
    Mockito.when(repository.findAll()).thenReturn(allItems);
    Mockito.when(queryParser.parse("wallet at the airport", knownPlaces))
        .thenReturn(new ParsedQuery("Airport", null, null, List.of()));

    List<LostItem> result = service.query("wallet at the airport");

    assertThat(result).containsExactly(matching);
  }

}
