package nl.fred.lostandfound.web.dto;

import java.util.List;

public record LostItemClaimsReportResponse(
    Long lostItemId,
    String itemName,
    int quantity,
    String place,
    List<ClaimantResponse> claimants
) {

  // SpotBugs EI_EXPOSE_REP/EI_EXPOSE_REP2: defensive copy here means the
  // accessor can safely return the field directly - no one can mutate our
  // internal list, neither through the constructor argument nor the getter.
  public LostItemClaimsReportResponse {
    claimants = List.copyOf(claimants);
  }

}
