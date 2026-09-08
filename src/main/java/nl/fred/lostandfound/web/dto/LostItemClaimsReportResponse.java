package nl.fred.lostandfound.web.dto;

import java.util.List;

public record LostItemClaimsReportResponse(
    Long lostItemId,
    String itemName,
    int quantity,
    String place,
    List<ClaimantResponse> claimants
) { }
