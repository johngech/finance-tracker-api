package com.marakicode.financetracker.common;

@FunctionalInterface
public interface CurrentUserProvider {
    Long getCurrentUserId();
}
