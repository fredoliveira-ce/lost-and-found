package nl.fred.lostandfound.web.dto;

import java.time.Instant;

public record ClaimResponse(
    Long id,
    Long lostItemId,
    Long userId,
    int quantity,
    Instant claimedAt
) { }
