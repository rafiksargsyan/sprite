package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
  private final AuthenticationConfiguration authConfig;
  private final CustomApiKeyAuthenticationProvider apiKeyAuthenticationProvider;

  @Autowired
  public SecurityConfig(AuthenticationConfiguration authConfig,
                        CustomApiKeyAuthenticationProvider apiKeyAuthenticationProvider) {
    this.authConfig = authConfig;
    this.apiKeyAuthenticationProvider = apiKeyAuthenticationProvider;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http)
      throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .authenticationProvider(apiKeyAuthenticationProvider)
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .addFilterBefore(new ApiKeyAuthenticationFilter(authConfig.getAuthenticationManager()),
            BearerTokenAuthenticationFilter.class)
        .build();
  }
}

