package nl.fred.lostandfound.web.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.fred.lostandfound.domain.entity.Claim;
import nl.fred.lostandfound.domain.service.ClaimService;
import nl.fred.lostandfound.domain.service.LostItemService;
import nl.fred.lostandfound.web.dto.ClaimRequest;
import nl.fred.lostandfound.web.dto.ClaimResponse;
import nl.fred.lostandfound.web.dto.LostItemResponse;
import nl.fred.lostandfound.web.mapper.LostItemResponseMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lost-items")
public class LostItemController {

  private final LostItemService lostItemService;
  private final ClaimService claimService;
  private final LostItemResponseMapper mapper;

  @ResponseStatus(HttpStatus.OK)
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  public List<LostItemResponse> findAll() {
    log.info("Request to list all lost items.");
    return mapper.toResponses(lostItemService.findAll());
  }

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(value = "/{id}/claims", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public ClaimResponse claim(@PathVariable Long id, @Valid @RequestBody ClaimRequest request) {
    log.info("User {} requests to claim {} of lost item {}.", request.userId(), request.quantity(), id);
    Claim claim = claimService.claim(id, request.userId(), request.quantity());
    return toResponse(claim);
  }

  private ClaimResponse toResponse(Claim claim) {
    return new ClaimResponse(
        claim.getId(),
        claim.getLostItem().getId(),
        claim.getUserId(),
        claim.getQuantity(),
        claim.getClaimedAt()
    );
  }

}
