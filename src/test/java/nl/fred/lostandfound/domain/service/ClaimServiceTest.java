package nl.fred.lostandfound.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import nl.fred.lostandfound.data.repository.ClaimRepository;
import nl.fred.lostandfound.data.repository.LostItemRepository;
import nl.fred.lostandfound.domain.entity.Claim;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.domain.exception.InsufficientQuantityException;
import nl.fred.lostandfound.mock.LostItemMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@DisplayName("Runs all tests for ClaimService")
class ClaimServiceTest {

  private ClaimService service;
  private ClaimRepository claimRepository;
  private LostItemRepository lostItemRepository;

  @BeforeEach
  void beforeEach() {
    claimRepository = Mockito.mock(ClaimRepository.class);
    lostItemRepository = Mockito.mock(LostItemRepository.class);
    service = new ClaimService(claimRepository, new LostItemService(lostItemRepository));
  }

  @Test
  @DisplayName("should claim a quantity within what remains available")
  void claimSucceedsWithinRemainingQuantity() {
    LostItem lostItem = LostItemMock.getOneWithQuantity(4);
    Mockito.when(lostItemRepository.findByIdForUpdate(lostItem.getId())).thenReturn(Optional.of(lostItem));
    Mockito.when(claimRepository.sumQuantityByLostItemId(lostItem.getId())).thenReturn(1);
    Mockito.when(claimRepository.save(Mockito.any(Claim.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Claim result = service.claim(lostItem.getId(), 1001L, 2);

    assertThat(result.getLostItem()).isEqualTo(lostItem);
    assertThat(result.getUserId()).isEqualTo(1001L);
    assertThat(result.getQuantity()).isEqualTo(2);

    ArgumentCaptor<Claim> captor = ArgumentCaptor.forClass(Claim.class);
    Mockito.verify(claimRepository).save(captor.capture());
    assertThat(captor.getValue().getClaimedAt()).isNotNull();
  }

  @Test
  @DisplayName("should reject a claim that exceeds the remaining quantity")
  void claimFailsWhenExceedingRemainingQuantity() {
    LostItem lostItem = LostItemMock.getOneWithQuantity(4);
    Mockito.when(lostItemRepository.findByIdForUpdate(lostItem.getId())).thenReturn(Optional.of(lostItem));
    Mockito.when(claimRepository.sumQuantityByLostItemId(lostItem.getId())).thenReturn(3);

    assertThatThrownBy(() -> service.claim(lostItem.getId(), 1001L, 2))
        .isInstanceOf(InsufficientQuantityException.class);

    Mockito.verify(claimRepository, Mockito.never()).save(Mockito.any());
  }

  @Test
  @DisplayName("should reject a claim for a lost item that does not exist")
  void claimFailsWhenLostItemDoesNotExist() {
    Mockito.when(lostItemRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.claim(1L, 1001L, 1))
        .isInstanceOf(nl.fred.lostandfound.domain.exception.LostItemNotFoundException.class);

    Mockito.verify(claimRepository, Mockito.never()).save(Mockito.any());
  }

}
