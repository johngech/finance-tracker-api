package com.marakicode.financetracker.auth;

public class NotAuthenticatedException extends RuntimeException {

    public NotAuthenticatedException(String message) {
        super(message);
    }
}
