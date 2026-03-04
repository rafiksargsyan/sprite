package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final ApiKeyAuthenticationFilter apiKeyFilter;

  @Autowired
  public SecurityConfig(ApiKeyAuthenticationFilter apiKeyFilter) {
    this.apiKeyFilter = apiKeyFilter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http)
      throws Exception {

    return http
        .csrf(AbstractHttpConfigurer::disable)

        .authorizeHttpRequests(auth -> {
//          auth.requestMatchers("/thumbnails-generation-job/**").permitAll();
          auth.anyRequest().authenticated();
        }
        )

        // OAuth2 JWT support
        .oauth2ResourceServer(oauth2 ->
            oauth2.jwt(Customizer.withDefaults()))

        // Add API key filter BEFORE JWT filter
//        .addFilterBefore(
//            apiKeyFilter,
//            BearerTokenAuthenticationFilter.class)

        .build();
  }
}

