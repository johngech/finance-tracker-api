package com.marakicode.financetracker.transactions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Transaction type", enumAsRef = true)
public enum TransactionType {
    INCOME,
    EXPENSE
}
