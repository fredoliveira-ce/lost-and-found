package nl.fred.lostandfound.web.dto;

public record LostItemResponse(
    Long id,
    String itemName,
    int quantity,
    int quantityRemaining,
    String place
) { }
