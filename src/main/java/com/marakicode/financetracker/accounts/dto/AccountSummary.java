package com.marakicode.financetracker.accounts.dto;

import com.marakicode.financetracker.accounts.AccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Admin-facing account summary with ownership info.
 */
public record AccountSummary(
    Long id,
    Long userId,
    String userName,
    String name,
    AccountType type,
    BigDecimal balance,
    String currency,
    boolean frozen,
    LocalDateTime createdAt
) {}
