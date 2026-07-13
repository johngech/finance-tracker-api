package com.marakicode.financetracker.accounts;

public class AccountTypeNotFoundException extends RuntimeException {

    public AccountTypeNotFoundException(String message) {
        super(message);
    }
}
