package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import com.rsargsyan.sprite.main_ctx.core.ports.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
//public class FilterConfig {
//
//  private final UserRepository userRepository;
//
//  @Autowired
//  public FilterConfig(UserRepository userRepository) {
//    this.userRepository = userRepository;
//  }
//
//  @Bean
//  public FilterRegistrationBean<ApiKeyAuthenticationFilter> customFilterRegistration() {
//    FilterRegistrationBean<ApiKeyAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
//
//    // Instantiate the filter using the injected repository
//    registrationBean.setFilter(new ApiKeyAuthenticationFilter(userRepository));
//
//    // Define URL patterns for the filter
//    registrationBean.addUrlPatterns("/api/*");
//
//    return registrationBean;
//  }
//}

