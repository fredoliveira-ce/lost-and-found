package nl.fred.lostandfound.loadtest;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

/**
 * Load test for the read-heavy "browse" path (login, list, search, query) -
 * the traffic pattern real usage is expected to be dominated by. Doesn't
 * touch the claim endpoint on purpose: claiming has limited stock per item,
 * so a repeatable load test would either exhaust it or need a reset step
 * between runs - out of scope for a first pass at this.
 */
public class LostAndFoundSimulation extends Simulation {

  private final HttpProtocolBuilder httpProtocol = http
      .baseUrl("http://localhost:8081")
      .acceptHeader("application/json")
      .contentTypeHeader("application/json");

  private final ChainBuilder login = exec(
      http("Login as alice")
          .post("/api/auth/login")
          .body(StringBody("{\"username\":\"alice\",\"password\":\"password123\"}"))
          .check(status().is(200))
          .check(jsonPath("$.token").saveAs("token")));

  private final ChainBuilder browseAndSearch = exec(login)
      .exec(http("List lost items")
          .get("/api/lost-items")
          .header("Authorization", "Bearer #{token}")
          .check(status().is(200)))
      .exec(http("Search lost items")
          .get("/api/lost-items/search?q=laptop")
          .header("Authorization", "Bearer #{token}")
          .check(status().is(200)))
      .exec(http("Natural-language query")
          .get("/api/lost-items/query?q=lost near the airport last week")
          .header("Authorization", "Bearer #{token}")
          .check(status().is(200)));

  private final ScenarioBuilder scn = scenario("Browse and search lost items").exec(browseAndSearch);

  {
    setUp(scn.injectOpen(atOnceUsers(5), rampUsers(50).during(30)))
        .protocols(httpProtocol);
  }

}
