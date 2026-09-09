package nl.fred.lostandfound.web.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.fred.lostandfound.domain.entity.Claim;
import nl.fred.lostandfound.domain.exception.InvalidTokenException;
import nl.fred.lostandfound.domain.service.ClaimService;
import nl.fred.lostandfound.domain.service.LostItemService;
import nl.fred.lostandfound.web.dto.ClaimRequest;
import nl.fred.lostandfound.web.dto.ClaimResponse;
import nl.fred.lostandfound.web.dto.LostItemResponse;
import nl.fred.lostandfound.web.mapper.LostItemResponseMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/search", produces = APPLICATION_JSON_VALUE)
  public List<LostItemResponse> search(@RequestParam(required = false) final String q) {
    log.info("Request to search lost items for query '{}'.", q);
    return mapper.toResponses(lostItemService.search(q));
  }

  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/query", produces = APPLICATION_JSON_VALUE)
  public List<LostItemResponse> query(@RequestParam(required = false) final String q) {
    log.info("Request to run a natural-language query '{}'.", q);
    return mapper.toResponses(lostItemService.query(q));
  }

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(value = "/{id}/claims", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public ClaimResponse claim(
      @PathVariable final Long id, @Valid @RequestBody final ClaimRequest request,
      @AuthenticationPrincipal final Jwt jwt) {
    final Long userId = extractUserId(jwt);
    log.info("User {} requests to claim {} of lost item {}.", userId, request.quantity(), id);
    final Claim claim = claimService.claim(id, userId, request.quantity());
    return toResponse(claim);
  }

  private Long extractUserId(final Jwt jwt) {
    final String uid = jwt.getClaimAsString("uid");
    if (uid == null) {
      throw new InvalidTokenException();
    }
    try {
      return Long.parseLong(uid);
    } catch (NumberFormatException _) {
      throw new InvalidTokenException();
    }
  }

  private ClaimResponse toResponse(final Claim claim) {
    return new ClaimResponse(
        claim.getId(),
        claim.getLostItem().getId(),
        claim.getUserId(),
        claim.getQuantity(),
        claim.getClaimedAt()
    );
  }

}
