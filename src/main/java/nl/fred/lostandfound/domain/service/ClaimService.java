package nl.fred.lostandfound.domain.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.fred.lostandfound.data.repository.ClaimRepository;
import nl.fred.lostandfound.data.repository.LostItemClaimedQuantity;
import nl.fred.lostandfound.domain.entity.Claim;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.domain.exception.InsufficientQuantityException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimService {

  private final ClaimRepository claimRepository;
  private final LostItemService lostItemService;

  @Transactional
  public Claim claim(final Long lostItemId, final Long userId, final int quantity) {
    final LostItem lostItem = lostItemService.findByIdForUpdate(lostItemId);

    final int alreadyClaimed = claimRepository.sumQuantityByLostItemId(lostItemId);
    final int remaining = lostItem.getQuantity() - alreadyClaimed;

    if (quantity > remaining) {
      log.warn("User {} tried to claim {} of lost item {} but only {} remain.",
          userId, quantity, lostItemId, remaining);
      throw new InsufficientQuantityException(lostItemId, quantity, remaining);
    }

    final Claim claim = Claim.builder()
        .lostItem(lostItem)
        .userId(userId)
        .quantity(quantity)
        .claimedAt(Instant.now())
        .build();

    return claimRepository.save(claim);
  }

  public List<Claim> findAll() {
    return claimRepository.findAll();
  }

  public Map<Long, Integer> sumClaimedQuantityByLostItem() {
    final Map<Long, Integer> result = new HashMap<>();
    for (final LostItemClaimedQuantity row : claimRepository.sumQuantityGroupedByLostItemId()) {
      result.put(row.getLostItemId(), row.getTotalQuantity().intValue());
    }
    return result;
  }

}
