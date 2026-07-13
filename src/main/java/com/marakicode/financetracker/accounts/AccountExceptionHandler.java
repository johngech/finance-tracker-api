package com.marakicode.financetracker.accounts;

import com.marakicode.financetracker.common.ErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AccountExceptionHandler {

    @ExceptionHandler(AccountTypeNotFoundException.class)
    public ResponseEntity<ErrorDto> handleAccountTypeNotFound(
            AccountTypeNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorDto.of(
                        HttpStatus.NOT_FOUND.value(),
                        "Not Found",
                        ex.getMessage(),
                        request.getRequestURI()));
    }
}
