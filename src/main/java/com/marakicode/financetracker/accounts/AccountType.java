package com.marakicode.financetracker.accounts;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Account type", enumAsRef = true)
public enum AccountType {
    CHECKING,
    SAVINGS,
    INVESTMENT
}
