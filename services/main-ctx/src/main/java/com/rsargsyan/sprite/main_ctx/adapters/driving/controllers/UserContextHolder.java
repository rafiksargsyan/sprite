package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

public class UserContextHolder {
  private static final ThreadLocal<UserContext> userContext = new ThreadLocal<>();

  public static void set(UserContext userCtx) {
    userContext.set(userCtx);
  }

  public static UserContext get() {
    return userContext.get();
  }

  public static void clear() {
    userContext.remove();
  }
}
