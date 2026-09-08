package nl.fred.lostandfound.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ClaimRequest(
    @NotNull(message = "must not be null") Long userId,
    @Min(value = 1, message = "must be at least 1") int quantity
) { }
