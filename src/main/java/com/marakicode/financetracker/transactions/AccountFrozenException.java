package com.marakicode.financetracker.transactions;

public class AccountFrozenException extends RuntimeException {

    public AccountFrozenException(String message) {
        super(message);
    }
}
