package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Profile("web")
@Configuration
@EnableWebSecurity
public class SecurityConfig {
  private final AuthenticationConfiguration authConfig;
  private final CustomApiKeyAuthenticationProvider apiKeyAuthenticationProvider;

  @Value("${cors.allowed-origins:*}")
  private List<String> allowedOrigins;

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
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/error", "/swagger-ui/**", "/v3/api-docs/**", "/thumbnails-generation-job/limits").permitAll()
            .anyRequest().authenticated())
        .authenticationProvider(apiKeyAuthenticationProvider)
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .addFilterBefore(new ApiKeyAuthenticationFilter(authConfig.getAuthenticationManager()),
            BearerTokenAuthenticationFilter.class)
        .build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
