package com.fittracker.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import reactor.core.publisher.Mono;

@Configuration
public class SecurityConfig {

  // geen redirects, gewoon 401 voor Postman
  @Bean
  @Order(1)
  SecurityWebFilterChain apiChain(ServerHttpSecurity http) {
    return http
      .securityMatcher(new PathPatternParserServerWebExchangeMatcher("/api/**"))
      .csrf(ServerHttpSecurity.CsrfSpec::disable)
      .authorizeExchange(auth -> auth
        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .pathMatchers("/actuator/**").permitAll()
        .anyExchange().authenticated()
      )
      .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
      .exceptionHandling(e -> e
        .authenticationEntryPoint((exchange, ex) ->
          Mono.fromRunnable(() -> exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED))
        )
      )
      .build();
  }

  // OAuth2 login redirect (voor je browser)
  @Bean
  @Order(2)
  SecurityWebFilterChain uiChain(ServerHttpSecurity http) {
    return http
      .csrf(ServerHttpSecurity.CsrfSpec::disable)
      .authorizeExchange(auth -> auth
        .pathMatchers("/actuator/**").permitAll()
        .pathMatchers("/login/**", "/oauth2/**").permitAll()
        .anyExchange().authenticated()
      )
      .oauth2Login(Customizer.withDefaults())
      .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()))
      .build();
  }
}
