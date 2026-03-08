package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
  private AuthenticationManager authManager;
  public ApiKeyAuthenticationFilter(AuthenticationManager authenticationManager) {
    authManager = authenticationManager;
  }
  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {
    String apiKey = request.getHeader("X-API-KEY");

    if (apiKey != null) {
      SecurityContextHolder.getContext().setAuthentication(authManager.authenticate(new CustomApiKey(apiKey)));
    }

    filterChain.doFilter(request, response);
  }
}
