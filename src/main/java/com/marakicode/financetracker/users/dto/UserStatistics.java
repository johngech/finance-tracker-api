package com.marakicode.financetracker.users.dto;

/**
 * System-wide user statistics for the admin dashboard.
 */
public record UserStatistics(
    long totalUsers,
    long activeUsers,
    long suspendedUsers,
    long adminCount,
    long regularUserCount
) {}
