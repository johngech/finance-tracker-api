package com.marakicode.financetracker.reports;

import com.marakicode.financetracker.common.ApiResponse;
import com.marakicode.financetracker.common.ErrorDto;
import com.marakicode.financetracker.reports.dto.AccountBreakdownResponse;
import com.marakicode.financetracker.reports.dto.CategoryBreakdownResponse;
import com.marakicode.financetracker.reports.dto.MonthlyBreakdownResponse;
import com.marakicode.financetracker.reports.dto.SummaryResponse;
import com.marakicode.financetracker.transactions.TransactionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Financial report and aggregation endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class ReportsController {

    private final ReportsService reportsService;

    @GetMapping("/summary")
    @Operation(summary = "Get income/expense summary", description = "Returns total income, total expense, net balance, and transaction count for the authenticated user within an optional date range.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Summary data",
                    content = @Content(schema = @Schema(implementation = SummaryResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range (from after to)")
    })
    public ResponseEntity<ApiResponse<SummaryResponse>> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        validateDateRange(from, to);
        SummaryResponse response = reportsService.getSummary(from, to);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/by-category")
    @Operation(summary = "Get category breakdown", description = "Returns spending breakdown by category, with percentage of total, for the authenticated user. Optionally filter by transaction type and date range.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category breakdown list",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CategoryBreakdownResponse.class)))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range (from after to)")
    })
    public ResponseEntity<ApiResponse<List<CategoryBreakdownResponse>>> getCategoryBreakdown(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        validateDateRange(from, to);
        List<CategoryBreakdownResponse> response = reportsService.getCategoryBreakdown(type, from, to);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/monthly")
    @Operation(summary = "Get monthly breakdown", description = "Returns month-by-month income vs expense for a given year for the authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Monthly breakdown list",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = MonthlyBreakdownResponse.class)))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing required 'year' parameter")
    })
    public ResponseEntity<ApiResponse<List<MonthlyBreakdownResponse>>> getMonthlyBreakdown(
            @RequestParam int year) {
        List<MonthlyBreakdownResponse> response = reportsService.getMonthlyBreakdown(year);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/by-account")
    @Operation(summary = "Get account breakdown", description = "Returns per-account income, expense, and net amount for the authenticated user within an optional date range.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account breakdown list",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AccountBreakdownResponse.class)))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range (from after to)")
    })
    public ResponseEntity<ApiResponse<List<AccountBreakdownResponse>>> getAccountBreakdown(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        validateDateRange(from, to);
        List<AccountBreakdownResponse> response = reportsService.getAccountBreakdown(from, to);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date must not be after 'to' date");
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDto> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorDto.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Bad Request",
                        ex.getMessage(),
                        request.getRequestURI()));
    }
}
