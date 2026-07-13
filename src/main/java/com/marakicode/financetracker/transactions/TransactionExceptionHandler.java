package com.marakicode.financetracker.transactions;

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
public class TransactionExceptionHandler {

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorDto> handleInsufficientFunds(
            InsufficientFundsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorDto.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(AccountFrozenException.class)
    public ResponseEntity<ErrorDto> handleAccountFrozen(
            AccountFrozenException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorDto.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        ex.getMessage(),
                        request.getRequestURI()));
    }
}
