package nl.fred.lostandfound.web.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import nl.fred.lostandfound.LostAndFoundApplication;
import nl.fred.lostandfound.data.repository.AccountRepository;
import nl.fred.lostandfound.data.repository.ClaimRepository;
import nl.fred.lostandfound.data.repository.LostItemRepository;
import nl.fred.lostandfound.domain.entity.Claim;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.mock.ClaimMock;
import nl.fred.lostandfound.mock.LostItemMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@SpringBootTest(classes = LostAndFoundApplication.class)
@Transactional
@DisplayName("Runs all tests for the admin lost item endpoints")
class AdminLostItemControllerIT {

  private static final String ADMIN_LOST_ITEMS_PATH = "/api/admin/lost-items";

  @Autowired private MockMvc mockMvc;
  @Autowired private LostItemRepository lostItemRepository;
  @Autowired private ClaimRepository claimRepository;
  @Autowired private AccountRepository accountRepository;

  private static RequestPostProcessor asAdmin() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  private static RequestPostProcessor asUser() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  @Nested
  @DisplayName("POST /api/admin/lost-items/import")
  class Import {

    @Test
    @DisplayName("should import lost items from an uploaded text file")
    void importsFromTextFile() throws Exception {
      String content = """
          ItemName: Laptop
          Quantity: 1
          Place: Taxi

          ItemName: Headphones
          Quantity: 2
          Place: Railway station
          """;

      MockMultipartFile file =
          new MockMultipartFile("file", "lost-items.txt", "text/plain", content.getBytes());

      mockMvc.perform(multipart(ADMIN_LOST_ITEMS_PATH + "/import").file(file).with(asAdmin()))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$", hasSize(2)))
          .andExpect(jsonPath("$[0].itemName", is("Laptop")))
          .andExpect(jsonPath("$[0].place", is("Taxi")))
          .andExpect(jsonPath("$[1].itemName", is("Headphones")));
    }

    @Test
    @DisplayName("should import lost items from an uploaded CSV file")
    void importsFromCsvFile() throws Exception {
      String content = """
          ItemName,Quantity,Place
          Jewels,4,Airport
          Laptop,1,Airport
          """;

      MockMultipartFile file =
          new MockMultipartFile("file", "lost-items.csv", "text/csv", content.getBytes());

      mockMvc.perform(multipart(ADMIN_LOST_ITEMS_PATH + "/import").file(file).with(asAdmin()))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$", hasSize(2)))
          .andExpect(jsonPath("$[0].itemName", is("Jewels")))
          .andExpect(jsonPath("$[0].quantity", is(4)))
          .andExpect(jsonPath("$[1].itemName", is("Laptop")));
    }

    @Test
    @DisplayName("should reject a file type with no supporting parser")
    void rejectsUnsupportedFileType() throws Exception {
      MockMultipartFile file =
          new MockMultipartFile("file", "lost-items.pdf", "application/pdf", "not really a pdf".getBytes());

      mockMvc.perform(multipart(ADMIN_LOST_ITEMS_PATH + "/import").file(file).with(asAdmin()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should reject an empty file")
    void rejectsEmptyFile() throws Exception {
      MockMultipartFile file =
          new MockMultipartFile("file", "lost-items.txt", "text/plain", new byte[0]);

      mockMvc.perform(multipart(ADMIN_LOST_ITEMS_PATH + "/import").file(file).with(asAdmin()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 401 when no token is provided")
    void requiresAuthentication() throws Exception {
      MockMultipartFile file =
          new MockMultipartFile("file", "lost-items.txt", "text/plain", "content".getBytes());

      mockMvc.perform(multipart(ADMIN_LOST_ITEMS_PATH + "/import").file(file))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 403 for a non-admin token")
    void rejectsNonAdminToken() throws Exception {
      MockMultipartFile file =
          new MockMultipartFile("file", "lost-items.txt", "text/plain", "content".getBytes());

      mockMvc.perform(multipart(ADMIN_LOST_ITEMS_PATH + "/import").file(file).with(asUser()))
          .andExpect(status().isForbidden());
    }

  }

  @Nested
  @DisplayName("GET /api/admin/lost-items/claims")
  class FindAllWithClaimants {

    @Test
    @DisplayName("should list lost items together with the users who claimed them")
    void listsLostItemsWithClaimants() throws Exception {
      Long aliceUserId = accountRepository.findByUsername("alice").orElseThrow().getUser().getId();
      LostItem lostItem = lostItemRepository.save(LostItemMock.getOneWithQuantity(4));
      Claim claim = claimRepository.save(ClaimMock.getOne(lostItem, aliceUserId));

      mockMvc.perform(get(ADMIN_LOST_ITEMS_PATH + "/claims").with(asAdmin()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(1)))
          .andExpect(jsonPath("$[0].lostItemId", is(lostItem.getId()), Long.class))
          .andExpect(jsonPath("$[0].claimants", hasSize(1)))
          .andExpect(jsonPath("$[0].claimants[0].userId", is(claim.getUserId()), Long.class))
          .andExpect(jsonPath("$[0].claimants[0].name", is("Alice Johnson")))
          .andExpect(jsonPath("$[0].claimants[0].quantity", is(claim.getQuantity())));
    }

    @Test
    @DisplayName("should list a lost item with no claimants as an empty list")
    void listsLostItemWithNoClaimants() throws Exception {
      LostItem lostItem = lostItemRepository.save(LostItemMock.getOne());

      mockMvc.perform(get(ADMIN_LOST_ITEMS_PATH + "/claims").with(asAdmin()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$[0].lostItemId", is(lostItem.getId()), Long.class))
          .andExpect(jsonPath("$[0].claimants", hasSize(0)));
    }

    @Test
    @DisplayName("should return 401 when no token is provided")
    void requiresAuthentication() throws Exception {
      mockMvc.perform(get(ADMIN_LOST_ITEMS_PATH + "/claims"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 403 for a non-admin token")
    void rejectsNonAdminToken() throws Exception {
      mockMvc.perform(get(ADMIN_LOST_ITEMS_PATH + "/claims").with(asUser()))
          .andExpect(status().isForbidden());
    }

  }

}
