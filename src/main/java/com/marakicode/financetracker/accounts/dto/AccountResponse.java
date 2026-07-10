package com.marakicode.financetracker.accounts.dto;

import com.marakicode.financetracker.accounts.AccountType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
    Long id,
    String name,
    AccountType type,
    BigDecimal balance,
    String currency,
    LocalDateTime createdAt
) {}
