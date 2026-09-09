package nl.fred.lostandfound.web.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import nl.fred.lostandfound.LostAndFoundApplication;
import nl.fred.lostandfound.web.dto.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unlike the other *IT classes (which fabricate a JWT directly via the test
 * support's jwt() post-processor), this exercises the real login flow end to
 * end: password hashing/matching in AuthService and a real, self-issued token
 * that then actually authenticates a follow-up request.
 */
@AutoConfigureMockMvc
@SpringBootTest(classes = LostAndFoundApplication.class)
@Transactional
@DisplayName("Runs all tests for the auth endpoints")
class AuthControllerIT {

  private static final String LOGIN_PATH = "/api/auth/login";

  @Autowired private MockMvc mockMvc;
  @Autowired private JsonMapper mapper;

  @Nested
  @DisplayName("POST /api/auth/login")
  class Login {

    @Test
    @DisplayName("should issue a token for a seeded account with the correct password")
    void loginSucceeds() throws Exception {
      mockMvc.perform(post(LOGIN_PATH)
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(new LoginRequest("alice", "password123"))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.token", not(is(""))));
    }

    @Test
    @DisplayName("should reject the wrong password")
    void loginFailsWithWrongPassword() throws Exception {
      mockMvc.perform(post(LOGIN_PATH)
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(new LoginRequest("alice", "not-the-password"))))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should reject an unknown username")
    void loginFailsWithUnknownUsername() throws Exception {
      mockMvc.perform(post(LOGIN_PATH)
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(new LoginRequest("nobody", "password123"))))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 400 for a blank username or password")
    void loginFailsForBlankCredentials() throws Exception {
      mockMvc.perform(post(LOGIN_PATH)
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(new LoginRequest("", ""))))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should issue a token that actually authenticates a follow-up request")
    void issuedTokenAuthenticatesFollowUpRequest() throws Exception {
      String response = mockMvc.perform(post(LOGIN_PATH)
              .contentType(MediaType.APPLICATION_JSON)
              .content(mapper.writeValueAsString(new LoginRequest("alice", "password123"))))
          .andExpect(status().isOk())
          .andReturn().getResponse().getContentAsString();

      String token = mapper.readTree(response).get("token").asString();

      mockMvc.perform(get("/api/lost-items").header("Authorization", "Bearer " + token))
          .andExpect(status().isOk());
    }

  }

}
