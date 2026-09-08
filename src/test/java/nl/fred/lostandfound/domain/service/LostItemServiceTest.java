package nl.fred.lostandfound.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import nl.fred.lostandfound.data.repository.LostItemRepository;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.domain.exception.LostItemNotFoundException;
import nl.fred.lostandfound.mock.LostItemMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("Runs all tests for LostItemService")
class LostItemServiceTest {

  private LostItemService service;
  private LostItemRepository repository;

  @BeforeEach
  void beforeEach() {
    repository = Mockito.mock(LostItemRepository.class);
    service = new LostItemService(repository);
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

}
