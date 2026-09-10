package nl.fred.lostandfound.mock;

import java.time.Instant;
import nl.fred.lostandfound.domain.entity.Claim;
import nl.fred.lostandfound.domain.entity.LostItem;

public class ClaimMock {

  public static Claim getOne(LostItem lostItem, Long userId) {
    return Claim.builder()
        .lostItem(lostItem)
        .userId(userId)
        .quantity(1)
        .claimedAt(Instant.now())
        .build();
  }

}
