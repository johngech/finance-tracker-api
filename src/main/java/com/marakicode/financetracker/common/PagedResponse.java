package com.marakicode.financetracker.common;

import java.util.List;

public record PagedResponse<T>(
    List<T> content,
    int page,
    int size,
    int count,
    int totalPages
) {}