package com.marakicode.financetracker.users;

import com.marakicode.financetracker.common.ErrorDto;
import com.marakicode.financetracker.users.exceptions.LastAdminActionException;
import com.marakicode.financetracker.users.exceptions.PasswordMismatchException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserExceptionHandler {

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorDto> handlePasswordMismatch(
            PasswordMismatchException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorDto.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Password Mismatch",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(LastAdminActionException.class)
    public ResponseEntity<ErrorDto> handleLastAdminAction(
            LastAdminActionException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorDto.of(
                        HttpStatus.CONFLICT.value(),
                        "Conflict",
                        ex.getMessage(),
                        request.getRequestURI()));
    }
}
