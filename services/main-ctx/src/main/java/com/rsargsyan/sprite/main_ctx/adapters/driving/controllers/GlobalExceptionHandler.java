package com.rsargsyan.sprite.main_ctx.adapters.driving.controllers;

import com.rsargsyan.sprite.main_ctx.core.exception.AuthorizationException;
import com.rsargsyan.sprite.main_ctx.core.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  public record ErrorResponse(String code, String message) {}

  @ExceptionHandler(AuthorizationException.class)
  public ResponseEntity<ErrorResponse> handleAuthZException(AuthorizationException e) {
    return new ResponseEntity<>(new ErrorResponse(e.getClass().getSimpleName(), e.getMessage()),
        HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ErrorResponse> handleGenericDomainException(DomainException e) {
    return new ResponseEntity<>(new ErrorResponse(e.getClass().getSimpleName(), e.getMessage()),
        HttpStatus.BAD_REQUEST);
  }
}
