package nl.fred.lostandfound.mock;

import java.time.Instant;
import nl.fred.lostandfound.domain.entity.Claim;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.domain.entity.User;

public class ClaimMock {

  public static Claim getOne(LostItem lostItem, Long userId) {
    return Claim.builder()
        .lostItem(lostItem)
        .user(User.builder().id(userId).build())
        .quantity(1)
        .claimedAt(Instant.now())
        .build();
  }

}
