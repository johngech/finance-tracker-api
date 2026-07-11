package com.marakicode.financetracker.admin.dto;

import com.marakicode.financetracker.accounts.dto.AccountStatistics;
import com.marakicode.financetracker.transactions.dto.TransactionStatistics;
import com.marakicode.financetracker.users.dto.UserStatistics;

public record DashboardResponse(
    UserStatistics userStats,
    AccountStatistics accountStats,
    TransactionStatistics transactionStats
) {}
