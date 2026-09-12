package nl.fred.lostandfound.web.mapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import nl.fred.lostandfound.domain.client.UserInfo;
import nl.fred.lostandfound.domain.client.UserServiceClient;
import nl.fred.lostandfound.domain.entity.Claim;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.domain.service.ClaimService;
import nl.fred.lostandfound.web.dto.ClaimantResponse;
import nl.fred.lostandfound.web.dto.LostItemClaimsReportResponse;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LostItemClaimsReportMapper {

  private final ClaimService claimService;
  private final UserServiceClient userServiceClient;

  public List<LostItemClaimsReportResponse> toReports(final List<LostItem> lostItems) {
    final List<Claim> claims = claimService.findAll();

    final Map<Long, List<Claim>> claimsByLostItemId = claims.stream()
        .collect(Collectors.groupingBy(claim -> claim.getLostItem().getId()));

    final Map<Long, UserInfo> usersById = claims.stream()
        .map(Claim::getUserId)
        .distinct()
        .collect(Collectors.toMap(Function.identity(), userServiceClient::findUser));

    return lostItems.stream()
        .map(lostItem -> toReport(lostItem, claimsByLostItemId.getOrDefault(lostItem.getId(), List.of()), usersById))
        .toList();
  }

  private LostItemClaimsReportResponse toReport(
      final LostItem lostItem, final List<Claim> claims, final Map<Long, UserInfo> usersById) {

    final List<ClaimantResponse> claimants = claims.stream()
        .map(claim -> new ClaimantResponse(
            claim.getUserId(),
            usersById.get(claim.getUserId()).name(),
            claim.getQuantity()))
        .toList();

    return new LostItemClaimsReportResponse(
        lostItem.getId(),
        lostItem.getItemName(),
        lostItem.getQuantity(),
        lostItem.getPlace(),
        claimants);
  }

}
