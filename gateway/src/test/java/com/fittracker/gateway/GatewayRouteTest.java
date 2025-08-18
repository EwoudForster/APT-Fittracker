package com.fittracker.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Integration test voor de Gateway routes met WireMock als backend.
 * In deze test schakelen we security uit via een test-only SecurityWebFilterChain
 * zodat we 401's vermijden en enkel de routing/functie testen.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(GatewayRouteTest.NoSecurityConfig.class)
class GatewayRouteTest {

  // WireMock als fake backend waar gateway naartoe doorstuurt
  static WireMockServer backend = new WireMockServer(options().dynamicPort());

  @BeforeAll
  static void startBackend() { backend.start(); }

  @AfterAll
  static void stopBackend() { backend.stop(); }

  @DynamicPropertySource
  static void gatewayProps(DynamicPropertyRegistry reg) {
    // users
    reg.add("spring.cloud.gateway.routes[0].id", () -> "users");
    reg.add("spring.cloud.gateway.routes[0].uri", () -> "http://localhost:" + backend.port());
    reg.add("spring.cloud.gateway.routes[0].predicates[0]", () -> "Path=/api/users/**");
    reg.add("spring.cloud.gateway.routes[0].filters[0]", () -> "StripPrefix=1");

    // workouts
    reg.add("spring.cloud.gateway.routes[1].id", () -> "workouts");
    reg.add("spring.cloud.gateway.routes[1].uri", () -> "http://localhost:" + backend.port());
    reg.add("spring.cloud.gateway.routes[1].predicates[0]", () -> "Path=/api/workouts/**");
    reg.add("spring.cloud.gateway.routes[1].filters[0]", () -> "StripPrefix=1");

    // progress
    reg.add("spring.cloud.gateway.routes[2].id", () -> "progress");
    reg.add("spring.cloud.gateway.routes[2].uri", () -> "http://localhost:" + backend.port());
    reg.add("spring.cloud.gateway.routes[2].predicates[0]", () -> "Path=/api/progress/**");
    reg.add("spring.cloud.gateway.routes[2].filters[0]", () -> "StripPrefix=1");

    // CORS configuratie
    reg.add("spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedOrigins[0]", () -> "http://localhost:4200");
    reg.add("spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedMethods[0]", () -> "GET");
    reg.add("spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedMethods[1]", () -> "POST");
    reg.add("spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedMethods[2]", () -> "PUT");
    reg.add("spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedMethods[3]", () -> "DELETE");
    reg.add("spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedHeaders[0]", () -> "*");
    reg.add("spring.cloud.gateway.default-filters[0]", () -> "DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin");
  }

  @Autowired WebTestClient webClient;
  @LocalServerPort int port;

  @Test
  void forwardsUsersGet_withStripPrefix() {
    backend.stubFor(get(urlEqualTo("/users/ping"))
        .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("{\"ok\":true}")));

    webClient.get()
        .uri("http://localhost:" + port + "/api/users/ping")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentType("application/json")
        .expectBody().json("{\"ok\":true}");

    backend.verify(getRequestedFor(urlEqualTo("/users/ping")));
  }

  @Test
  void forwardsWorkoutsPost_andPreservesStatus() {
    backend.stubFor(post(urlEqualTo("/workouts"))
        .withRequestBody(equalToJson("{\"userId\":\"u1\"}", true, true))
        .willReturn(aResponse()
            .withStatus(201)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"id\":\"w1\"}")));

    webClient.post()
        .uri("http://localhost:" + port + "/api/workouts")
        .header("Content-Type", "application/json")
        .bodyValue("{\"userId\":\"u1\"}")
        .exchange()
        .expectStatus().isCreated()
        .expectBody().json("{\"id\":\"w1\"}");

    backend.verify(postRequestedFor(urlEqualTo("/workouts")));
  }

  @Test
  void forwardsProgressPut() {
    backend.stubFor(put(urlEqualTo("/progress/111/increment"))
        .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("{\"ok\":true}")));

    webClient.put()
        .uri("http://localhost:" + port + "/api/progress/111/increment")
        .exchange()
        .expectStatus().isOk()
        .expectBody().json("{\"ok\":true}");

    backend.verify(putRequestedFor(urlEqualTo("/progress/111/increment")));
  }

  @Test
  void corsPreflightIsAllowed() {
    webClient.options()
        .uri("http://localhost:" + port + "/api/workouts")
        .header("Origin", "http://localhost:4200")
        .header("Access-Control-Request-Method", "POST")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:4200");
  }

  /**
   * Test-only security override: alles toestaan.
   * Dit voorkomt 401's tijdens integratietesten van de gateway routing.
   */
  @TestConfiguration
  static class NoSecurityConfig {
    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
      return http
          .csrf(ServerHttpSecurity.CsrfSpec::disable)
          .authorizeExchange(ex -> ex.anyExchange().permitAll())
          .build();
    }
  }
}
