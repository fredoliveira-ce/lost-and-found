package nl.fred.lostandfound.web.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import nl.fred.lostandfound.LostAndFoundApplication;
import nl.fred.lostandfound.data.repository.AccountRepository;
import nl.fred.lostandfound.data.repository.LostItemRepository;
import nl.fred.lostandfound.domain.entity.LostItem;
import nl.fred.lostandfound.mock.LostItemMock;
import nl.fred.lostandfound.web.dto.ClaimRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@AutoConfigureMockMvc
@SpringBootTest(classes = LostAndFoundApplication.class)
@Transactional
@DisplayName("Runs all tests for the user-facing lost item endpoints")
class LostItemControllerIT {

  private static final String LOST_ITEMS_PATH = "/api/lost-items";

  @Autowired private MockMvc mockMvc;
  @Autowired private JsonMapper mapper;
  @Autowired private LostItemRepository lostItemRepository;
  @Autowired private AccountRepository accountRepository;

  private Long userId;

  @BeforeEach
  void resolveUserId() {
    userId = accountRepository.findByUsername("alice").orElseThrow().getId();
  }

  @Nested
  @DisplayName("GET /api/lost-items")
  class FindAll {

    @Test
    @DisplayName("should return 401 when no token is provided")
    void findAllRequiresAuthentication() throws Exception {
      mockMvc.perform(get(LOST_ITEMS_PATH))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return an empty list when there are no lost items")
    void findAllWithNoResults() throws Exception {
      mockMvc.perform(get(LOST_ITEMS_PATH).with(asUser()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("should return the quantity remaining after a partial claim")
    void findAllReflectsRemainingQuantity() throws Exception {
      LostItem saved = lostItemRepository.save(LostItemMock.getOneWithQuantity(3));

      mockMvc.perform(post(LOST_ITEMS_PATH + "/" + saved.getId() + "/claims")
              .with(asUser())
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(new ClaimRequest(2))))
          .andExpect(status().isCreated());

      mockMvc.perform(get(LOST_ITEMS_PATH).with(asUser()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(1)))
          .andExpect(jsonPath("$[0].itemName", is(saved.getItemName())))
          .andExpect(jsonPath("$[0].quantity", is(3)))
          .andExpect(jsonPath("$[0].quantityRemaining", is(1)));
    }

  }

  @Nested
  @DisplayName("GET /api/lost-items/search")
  class Search {

    @Test
    @DisplayName("should return 401 when no token is provided")
    void searchRequiresAuthentication() throws Exception {
      mockMvc.perform(get(LOST_ITEMS_PATH + "/search").param("q", "laptop"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 400 when the query is missing")
    void searchRequiresQuery() throws Exception {
      mockMvc.perform(get(LOST_ITEMS_PATH + "/search").with(asUser()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 400 when the query is blank")
    void searchRejectsBlankQuery() throws Exception {
      mockMvc.perform(get(LOST_ITEMS_PATH + "/search").param("q", " ").with(asUser()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should find items tolerating a typo in the query")
    void searchFindsItemsWithTypoTolerance() throws Exception {
      LostItem laptop = lostItemRepository.save(LostItemMock.getOne("Laptop", "Airport"));
      lostItemRepository.save(LostItemMock.getOne("Umbrella", "Cafeteria"));

      mockMvc.perform(get(LOST_ITEMS_PATH + "/search").param("q", "labtop").with(asUser()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(1)))
          .andExpect(jsonPath("$[0].id", is(laptop.getId()), Long.class));
    }
  }

  @Nested
  @DisplayName("GET /api/lost-items/query")
  class Query {

    @Test
    @DisplayName("should return 401 when no token is provided")
    void queryRequiresAuthentication() throws Exception {
      mockMvc.perform(get(LOST_ITEMS_PATH + "/query").param("q", "laptop"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 400 when the query is missing")
    void queryRequiresQuery() throws Exception {
      mockMvc.perform(get(LOST_ITEMS_PATH + "/query").with(asUser()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 400 when the query is blank")
    void queryRejectsBlankQuery() throws Exception {
      mockMvc.perform(get(LOST_ITEMS_PATH + "/query").param("q", " ").with(asUser()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should filter items by a recognized place mentioned in the query")
    void queryFiltersByRecognizedPlace() throws Exception {
      LostItem laptop = lostItemRepository.save(LostItemMock.getOne("Laptop", "Airport"));
      lostItemRepository.save(LostItemMock.getOne("Wallet", "Cafeteria"));

      mockMvc.perform(get(LOST_ITEMS_PATH + "/query")
              .param("q", "lost near the airport")
              .with(asUser()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", hasSize(1)))
          .andExpect(jsonPath("$[0].id", is(laptop.getId()), Long.class));
    }
  }

  @Nested
  @DisplayName("POST /api/lost-items/{id}/claims")
  class Claim {

    @Test
    @DisplayName("should claim a lost item successfully")
    void claimSuccessfully() throws Exception {
      LostItem saved = lostItemRepository.save(LostItemMock.getOneWithQuantity(2));

      MockHttpServletRequestBuilder request = post(LOST_ITEMS_PATH + "/" + saved.getId() + "/claims")
          .with(asUser())
          .contentType(MediaType.APPLICATION_JSON)
          .content(mapper.writeValueAsString(new ClaimRequest(1)));

      mockMvc.perform(request)
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.lostItemId", is(saved.getId()), Long.class))
          .andExpect(jsonPath("$.userId", is(userId), Long.class))
          .andExpect(jsonPath("$.quantity", is(1)));
    }

    @Test
    @DisplayName("should return 401 when no token is provided")
    void claimRequiresAuthentication() throws Exception {
      LostItem saved = lostItemRepository.save(LostItemMock.getOneWithQuantity(1));

      MockHttpServletRequestBuilder request = post(LOST_ITEMS_PATH + "/" + saved.getId() + "/claims")
          .contentType(MediaType.APPLICATION_JSON)
          .content(mapper.writeValueAsString(new ClaimRequest(1)));

      mockMvc.perform(request)
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 404 when claiming a lost item that does not exist")
    void claimFailsWhenLostItemDoesNotExist() throws Exception {
      MockHttpServletRequestBuilder request = post(LOST_ITEMS_PATH + "/999/claims")
          .with(asUser())
          .contentType(MediaType.APPLICATION_JSON)
          .content(mapper.writeValueAsString(new ClaimRequest(1)));

      mockMvc.perform(request)
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should return 409 when claiming more than what remains")
    void claimFailsWhenExceedingRemainingQuantity() throws Exception {
      LostItem saved = lostItemRepository.save(LostItemMock.getOneWithQuantity(1));

      MockHttpServletRequestBuilder request = post(LOST_ITEMS_PATH + "/" + saved.getId() + "/claims")
          .with(asUser())
          .contentType(MediaType.APPLICATION_JSON)
          .content(mapper.writeValueAsString(new ClaimRequest(2)));

      mockMvc.perform(request)
          .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("should return 400 for an invalid claim request")
    void claimFailsForInvalidRequest() throws Exception {
      LostItem saved = lostItemRepository.save(LostItemMock.getOneWithQuantity(1));

      MockHttpServletRequestBuilder request = post(LOST_ITEMS_PATH + "/" + saved.getId() + "/claims")
          .with(asUser())
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"quantity\": 0}");

      mockMvc.perform(request)
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 401 when the token has no 'uid' claim")
    void claimFailsWhenTokenHasNoUid() throws Exception {
      LostItem saved = lostItemRepository.save(LostItemMock.getOneWithQuantity(1));

      MockHttpServletRequestBuilder request = post(LOST_ITEMS_PATH + "/" + saved.getId() + "/claims")
          .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(mapper.writeValueAsString(new ClaimRequest(1)));

      mockMvc.perform(request)
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.message", is("Token is missing or has an invalid 'uid' claim.")));
    }

    @Test
    @DisplayName("should return 401 when the token's 'uid' claim isn't a number")
    void claimFailsWhenUidIsNotANumber() throws Exception {
      LostItem saved = lostItemRepository.save(LostItemMock.getOneWithQuantity(1));

      MockHttpServletRequestBuilder request = post(LOST_ITEMS_PATH + "/" + saved.getId() + "/claims")
          .with(jwt()
              .jwt(builder -> builder.claim("uid", "not-a-number"))
              .authorities(new SimpleGrantedAuthority("ROLE_USER")))
          .contentType(MediaType.APPLICATION_JSON)
          .content(mapper.writeValueAsString(new ClaimRequest(1)));

      mockMvc.perform(request)
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.message", is("Token is missing or has an invalid 'uid' claim.")));
    }
  }

  private RequestPostProcessor asUser() {
    return jwt()
            .jwt(builder -> builder.claim("uid", userId))
            .authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }
}
