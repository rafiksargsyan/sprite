package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import com.rsargsyan.sprite.main_ctx.core.app.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Profile("web")
@Component
public class RequestInterceptorConfig implements WebMvcConfigurer {
  private final AuthService authService;

  @Autowired
  public RequestInterceptorConfig(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new UserContextInterceptor(authService));
  }
}
