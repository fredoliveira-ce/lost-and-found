package nl.fred.lostandfound.web.dto;

import jakarta.validation.constraints.Min;

public record ClaimRequest(
    @Min(value = 1, message = "must be at least 1") int quantity
) { }
