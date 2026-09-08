package nl.fred.lostandfound.web.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.fred.lostandfound.domain.client.UserInfo;
import nl.fred.lostandfound.domain.client.UserServiceClient;
import nl.fred.lostandfound.domain.entity.Claim;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.domain.service.ClaimService;
import nl.fred.lostandfound.domain.service.LostItemImportService;
import nl.fred.lostandfound.domain.service.LostItemService;
import nl.fred.lostandfound.web.dto.ClaimantResponse;
import nl.fred.lostandfound.web.dto.LostItemClaimsReportResponse;
import nl.fred.lostandfound.web.dto.LostItemResponse;
import nl.fred.lostandfound.web.mapper.LostItemResponseMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/lost-items")
public class AdminLostItemController {

  private final LostItemImportService importService;
  private final LostItemService lostItemService;
  private final ClaimService claimService;
  private final UserServiceClient userServiceClient;
  private final LostItemResponseMapper mapper;

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(value = "/import", consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
  public List<LostItemResponse> importLostItems(@RequestParam("file") MultipartFile file) {
    log.info("Admin request to import lost items from file '{}'.", file.getOriginalFilename());
    return mapper.toResponses(importService.importFrom(file));
  }

  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/claims", produces = APPLICATION_JSON_VALUE)
  public List<LostItemClaimsReportResponse> findAllWithClaimants() {
    log.info("Admin request to list all lost items with their claimants.");

    final List<Claim> claims = claimService.findAll();

    final Map<Long, List<Claim>> claimsByLostItemId = claims.stream()
        .collect(Collectors.groupingBy(claim -> claim.getLostItem().getId()));

    final Map<Long, UserInfo> usersById = claims.stream()
        .map(Claim::getUserId)
        .distinct()
        .collect(Collectors.toMap(Function.identity(), userServiceClient::findUser));

    return lostItemService.findAll().stream()
        .map(lostItem -> toReport(lostItem, claimsByLostItemId.getOrDefault(lostItem.getId(), List.of()), usersById))
        .toList();
  }

  private LostItemClaimsReportResponse toReport(
      LostItem lostItem, List<Claim> claims, Map<Long, UserInfo> usersById) {

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
        claimants
    );
  }

}
