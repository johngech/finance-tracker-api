package com.marakicode.financetracker.reports;

import com.marakicode.financetracker.common.ApiResponse;
import com.marakicode.financetracker.common.ErrorDto;
import com.marakicode.financetracker.reports.dto.AccountBreakdownResponse;
import com.marakicode.financetracker.reports.dto.CategoryBreakdownResponse;
import com.marakicode.financetracker.reports.dto.MonthlyBreakdownResponse;
import com.marakicode.financetracker.reports.dto.SummaryResponse;
import com.marakicode.financetracker.transactions.TransactionType;
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
public class ReportsController {

    private final ReportsService reportsService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<SummaryResponse>> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        validateDateRange(from, to);
        SummaryResponse response = reportsService.getSummary(from, to);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/by-category")
    public ResponseEntity<ApiResponse<List<CategoryBreakdownResponse>>> getCategoryBreakdown(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        validateDateRange(from, to);
        List<CategoryBreakdownResponse> response = reportsService.getCategoryBreakdown(type, from, to);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<List<MonthlyBreakdownResponse>>> getMonthlyBreakdown(
            @RequestParam int year) {
        List<MonthlyBreakdownResponse> response = reportsService.getMonthlyBreakdown(year);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/by-account")
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
