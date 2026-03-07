package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class UserContextInterceptor implements HandlerInterceptor {
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler) throws Exception {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof Jwt) {
      Map<String, Object> claims = ((Jwt) principal).getClaims();
      String externalId = (String) claims.get("sub");
      String accountId = request.getHeader("X-ACCOUNT-ID");
      UserContextHolder.set(UserContext.builder().externalId(externalId).accountId(accountId).build());
    } else {
      throw new RuntimeException("not support auth");
    }
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                              Object handler, Exception ex) throws Exception {
    UserContextHolder.clear();
  }
}

