package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import com.rsargsyan.sprite.main_ctx.core.ports.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

  private final UserRepository userRepository;

  @Autowired
  public ApiKeyAuthenticationFilter(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {

//    String header = request.getHeader("Authorization");
//
//    if (header != null && header.startsWith("ApiKey ")) {
//
//      String apiKey = header.substring(7);
//
//      userRepository.findByApiKey(apiKey)
//          .ifPresent(user -> {
//            UsernamePasswordAuthenticationToken auth =
//                new UsernamePasswordAuthenticationToken(user, null, null);
//            SecurityContextHolder.getContext()
//                .setAuthentication(auth);
//          });
//    }

    filterChain.doFilter(request, response);
  }
}

