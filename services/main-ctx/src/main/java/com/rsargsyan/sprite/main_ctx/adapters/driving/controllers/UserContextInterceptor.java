package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import com.rsargsyan.sprite.main_ctx.core.app.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

public class UserContextInterceptor implements HandlerInterceptor {
  private final AuthService authService;

  public UserContextInterceptor(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Object principal = auth.getPrincipal();
    if (principal instanceof Jwt jwt) {
      Map<String, Object> claims = jwt.getClaims();
      String externalId = (String) claims.get("sub");
      String accountId = request.getHeader("X-ACCOUNT-ID");
      String fullName = (String) claims.get("name");
      String userProfileId = accountId != null ? authService.getUserProfileId(externalId, accountId) : null;
      UserContextHolder.set(UserContext.builder().externalId(externalId)
          .accountId(accountId).fullName(fullName).userProfileId(userProfileId).build());
    } else if (auth instanceof CustomApiKey customApiKey) {
      var userContext = authService.getUserContextByApiKey(customApiKey.getApiKeyId());
      UserContextHolder.set(userContext);
    } else {
      assert false;
    }
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                              Object handler, Exception ex) {
    UserContextHolder.clear();
  }
}
