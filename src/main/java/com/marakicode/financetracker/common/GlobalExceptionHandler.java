package com.marakicode.financetracker.common;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.debug("Validation failed on {}: {}", request.getRequestURI(), ex.getMessage());
        List<ErrorDto.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorDto.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorDto.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Validation Failed",
                        "Request validation failed",
                        request.getRequestURI(),
                        fieldErrors));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDto> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.debug("Resource not found on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorDto.of(
                        HttpStatus.NOT_FOUND.value(),
                        "Not Found",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDto> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        log.debug("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorDto.of(
                        HttpStatus.FORBIDDEN.value(),
                        "Forbidden",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorDto> handleDisabledException(
            DisabledException ex, HttpServletRequest request) {
        log.debug("User disabled on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorDto.of(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Unauthorized",
                        "User account is disabled",
                        request.getRequestURI()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorDto> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {
        log.debug("Duplicate resource on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorDto.of(
                        HttpStatus.CONFLICT.value(),
                        "Conflict",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorDto> handleMethodNotAllowedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.debug("Method not allowed on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorDto.of(
                        HttpStatus.METHOD_NOT_ALLOWED.value(),
                        "Method Not Allowed",
                        ex.getMessage(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDto> handleMalformedRequest(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.debug("Malformed request body on {}: {}", request.getRequestURI(), ex.getMessage());

        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof InvalidFormatException ife && ife.getTargetType() != null
                && ife.getTargetType().isEnum()) {
            return buildInvalidEnumResponse(ife, request.getRequestURI());
        }

        return ResponseEntity.badRequest()
                .body(ErrorDto.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Malformed Request",
                        "Request body is malformed or unreadable",
                        request.getRequestURI()));
    }

    private ResponseEntity<ErrorDto> buildInvalidEnumResponse(
            InvalidFormatException ife, String requestUri) {
        String validValues = Arrays.stream(ife.getTargetType().getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        String fieldName = ife.getPath() != null && !ife.getPath().isEmpty()
                ? ife.getPath().get(ife.getPath().size() - 1).getFieldName()
                : "value";
        return ResponseEntity.badRequest()
                .body(ErrorDto.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Invalid Value",
                        String.format("Invalid value '%s' for field '%s'. Allowed values are: %s",
                                ife.getValue(), fieldName, validValues),
                        requestUri));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDto> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.debug("Type mismatch on {}: parameter '{}' cannot be converted to {}",
                request.getRequestURI(), ex.getName(), ex.getRequiredType() != null
                        ? ex.getRequiredType().getSimpleName() : "unknown");
        return ResponseEntity.badRequest()
                .body(ErrorDto.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Type Mismatch",
                        String.format("Parameter '%s' must be of type %s",
                                ex.getName(), ex.getRequiredType() != null
                                        ? ex.getRequiredType().getSimpleName() : "unknown"),
                        request.getRequestURI()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorDto> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.debug("Missing parameter on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorDto.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        String.format("Required parameter '%s' is missing", ex.getParameterName()),
                        request.getRequestURI()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorDto> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpServletRequest request) {
        log.debug("Resource not found on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorDto.of(
                        HttpStatus.NOT_FOUND.value(),
                        "Not Found",
                        "The requested resource was not found",
                        request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}: ", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorDto.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Internal Server Error",
                        "An unexpected error occurred",
                        request.getRequestURI()));
    }
}
