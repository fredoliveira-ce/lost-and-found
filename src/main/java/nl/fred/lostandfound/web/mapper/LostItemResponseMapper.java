package nl.fred.lostandfound.web.mapper;

import lombok.RequiredArgsConstructor;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.domain.service.ClaimService;
import nl.fred.lostandfound.web.dto.LostItemResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LostItemResponseMapper {

    private final ClaimService claimService;

    public List<LostItemResponse> toResponses(final List<LostItem> lostItems) {
        final Map<Long, Integer> claimedByLostItemId = claimService.sumClaimedQuantityByLostItem();

        return lostItems.stream()
                .map(lostItem -> toResponse(lostItem, claimedByLostItemId.getOrDefault(lostItem.getId(), 0)))
                .toList();
    }

    private LostItemResponse toResponse(final LostItem lostItem, final int claimedQuantity) {
        final int quantity = lostItem.getQuantity();
        final int quantityRemaining = quantity - claimedQuantity;

        return new LostItemResponse(
                lostItem.getId(), lostItem.getItemName(), quantity, quantityRemaining, lostItem.getPlace());
    }

}
