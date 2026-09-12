package nl.fred.lostandfound.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import nl.fred.lostandfound.domain.client.UserInfo;
import nl.fred.lostandfound.domain.client.UserServiceClient;
import nl.fred.lostandfound.domain.entity.Claim;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.domain.service.ClaimService;
import nl.fred.lostandfound.mock.ClaimMock;
import nl.fred.lostandfound.web.dto.LostItemClaimsReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@DisplayName("Runs all tests for LostItemClaimsReportMapper")
class LostItemClaimsReportMapperTest {

  private ClaimService claimService;
  private UserServiceClient userServiceClient;
  private LostItemClaimsReportMapper mapper;

  @BeforeEach
  void beforeEach() {
    claimService = Mockito.mock(ClaimService.class);
    userServiceClient = Mockito.mock(UserServiceClient.class);
    mapper = new LostItemClaimsReportMapper(claimService, userServiceClient);
  }

  @Test
  @DisplayName("should list a lost item with no claims as having no claimants")
  void listsLostItemWithNoClaimsAsEmpty() {
    LostItem lostItem = LostItem.builder().id(1L).itemName("Laptop").quantity(2).place("Airport").build();
    Mockito.when(claimService.findAll()).thenReturn(List.of());

    List<LostItemClaimsReportResponse> reports = mapper.toReports(List.of(lostItem));

    assertThat(reports).hasSize(1);
    assertThat(reports.get(0).lostItemId()).isEqualTo(1L);
    assertThat(reports.get(0).claimants()).isEmpty();
  }

  @Test
  @DisplayName("should resolve a claimant's name through the user service")
  void resolvesClaimantNameThroughUserService() {
    LostItem lostItem = LostItem.builder().id(1L).itemName("Laptop").quantity(2).place("Airport").build();
    Claim claim = ClaimMock.getOne(lostItem, 1001L);
    Mockito.when(claimService.findAll()).thenReturn(List.of(claim));
    Mockito.when(userServiceClient.findUser(1001L)).thenReturn(new UserInfo(1001L, "Alice Johnson"));

    List<LostItemClaimsReportResponse> reports = mapper.toReports(List.of(lostItem));

    assertThat(reports.get(0).claimants()).hasSize(1);
    assertThat(reports.get(0).claimants().get(0).userId()).isEqualTo(1001L);
    assertThat(reports.get(0).claimants().get(0).name()).isEqualTo("Alice Johnson");
    assertThat(reports.get(0).claimants().get(0).quantity()).isEqualTo(claim.getQuantity());
  }

  @Test
  @DisplayName("should only include claims that belong to each specific lost item")
  void onlyIncludesClaimsForTheMatchingLostItem() {
    LostItem laptop = LostItem.builder().id(1L).itemName("Laptop").quantity(2).place("Airport").build();
    LostItem wallet = LostItem.builder().id(2L).itemName("Wallet").quantity(1).place("Cafeteria").build();
    Claim laptopClaim = ClaimMock.getOne(laptop, 1001L);
    Mockito.when(claimService.findAll()).thenReturn(List.of(laptopClaim));
    Mockito.when(userServiceClient.findUser(1001L)).thenReturn(new UserInfo(1001L, "Alice Johnson"));

    List<LostItemClaimsReportResponse> reports = mapper.toReports(List.of(laptop, wallet));

    LostItemClaimsReportResponse laptopReport = reports.stream()
        .filter(report -> report.lostItemId().equals(1L)).findFirst().orElseThrow();
    LostItemClaimsReportResponse walletReport = reports.stream()
        .filter(report -> report.lostItemId().equals(2L)).findFirst().orElseThrow();

    assertThat(laptopReport.claimants()).hasSize(1);
    assertThat(walletReport.claimants()).isEmpty();
  }

  @Test
  @DisplayName("should look up each distinct claimant only once even with multiple claims")
  void looksUpEachDistinctClaimantOnlyOnce() {
    LostItem lostItem = LostItem.builder().id(1L).itemName("Laptop").quantity(5).place("Airport").build();
    Claim first = ClaimMock.getOne(lostItem, 1001L);
    Claim second = ClaimMock.getOne(lostItem, 1001L);
    Mockito.when(claimService.findAll()).thenReturn(List.of(first, second));
    Mockito.when(userServiceClient.findUser(1001L)).thenReturn(new UserInfo(1001L, "Alice Johnson"));

    List<LostItemClaimsReportResponse> reports = mapper.toReports(List.of(lostItem));

    assertThat(reports.get(0).claimants()).hasSize(2);
    Mockito.verify(userServiceClient, Mockito.times(1)).findUser(1001L);
  }

}
