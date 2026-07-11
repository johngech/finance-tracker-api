package com.marakicode.financetracker.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for updating account currency")
public record CurrencyUpdateRequest(
    @Schema(example = "EUR", description = "New 3-letter ISO currency code")
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code (e.g., USD)")
    String currency
) {}
