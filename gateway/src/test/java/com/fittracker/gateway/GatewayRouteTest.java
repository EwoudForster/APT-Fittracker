package com.fittracker.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRouteTest {

  // WireMock als fake backend waar gateway naar doorstuurt
  static WireMockServer backend = new WireMockServer(options().dynamicPort());

  @BeforeAll
  static void startBackend() { backend.start(); }

  @AfterAll
  static void stopBackend() { backend.stop(); }

  @DynamicPropertySource
  static void gatewayProps(DynamicPropertyRegistry reg) {
    // Dynamisch de routes configureren zodat gateway → WireMock gaat
    // usres
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

  // Test client die tegen de echte gateway (random port) schiet
  @Autowired WebTestClient webClient;
  @LocalServerPort int port;

  @Test
  void forwardsUsersGet_withStripPrefix() {
    // Backend stub: verwacht GET /users/ping
    backend.stubFor(get(urlEqualTo("/users/ping"))
        .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("{\"ok\":true}")));

    // Gateway call: client vraagt /api/users/ping
    webClient.get()
        .uri("http://localhost:" + port + "/api/users/ping")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentType("application/json")
        .expectBody().json("{\"ok\":true}");

    // Verifieer dat gateway correct heeft doorgestuurd naar backend
    backend.verify(getRequestedFor(urlEqualTo("/users/ping")));
  }

  @Test
  void forwardsWorkoutsPost_andPreservesStatus() {
    // Backend stub: POST /workouts met body {"userId":"u1"} → 201 + JSON response
    backend.stubFor(post(urlEqualTo("/workouts"))
        .withRequestBody(equalToJson("{\"userId\":\"u1\"}", true, true))
        .willReturn(aResponse()
            .withStatus(201)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"id\":\"w1\"}")));

    // Gateway call: /api/workouts met dezelfde body
    webClient.post()
        .uri("http://localhost:" + port + "/api/workouts")
        .header("Content-Type", "application/json")
        .bodyValue("{\"userId\":\"u1\"}")
        .exchange()
        .expectStatus().isCreated()
        .expectBody().json("{\"id\":\"w1\"}");

    // Check dat backend de juiste call ontving
    backend.verify(postRequestedFor(urlEqualTo("/workouts")));
  }

  @Test
  void forwardsProgressPut() {
    // Backend stub: PUT /progress/111/increment → JSON terug
    backend.stubFor(put(urlEqualTo("/progress/111/increment"))
        .willReturn(aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("{\"ok\":true}")));

    // Gateway call: /api/progress/111/increment
    webClient.put()
        .uri("http://localhost:" + port + "/api/progress/111/increment")
        .exchange()
        .expectStatus().isOk()
        .expectBody().json("{\"ok\":true}");

    backend.verify(putRequestedFor(urlEqualTo("/progress/111/increment")));
  }

  @Test
  void corsPreflightIsAllowed() {
    // Test preflight OPTIONS-request → moet 200 + juiste headers teruggeven
    webClient.options()
        .uri("http://localhost:" + port + "/api/workouts")
        .header("Origin", "http://localhost:4200")
        .header("Access-Control-Request-Method", "POST")
        .exchange()
        .expectStatus().isOk()
        .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:4200");
  }
}
