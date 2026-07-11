package com.marakicode.financetracker.accounts.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * System-wide account statistics for the admin dashboard.
 */
public record AccountStatistics(
    long totalAccounts,
    long frozenAccounts,
    BigDecimal totalBalance,
    Map<String, Long> accountsByType
) {}
