package com.marakicode.financetracker.auth;

import com.marakicode.financetracker.common.ErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthExceptionHandler {

    @ExceptionHandler(NotAuthenticatedException.class)
    public ResponseEntity<ErrorDto> handleNotAuthenticated(
            NotAuthenticatedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorDto.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Unauthorized",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(InvalidJwtAuthenticationException.class)
    public ResponseEntity<ErrorDto> handleInvalidJwtAuthentication(
            InvalidJwtAuthenticationException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorDto.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Unauthorized",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorDto> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorDto.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Unauthorized",
                        "Invalid email or password",
                        request.getRequestURI()));
    }
}
