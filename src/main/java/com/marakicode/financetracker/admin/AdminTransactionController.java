package com.marakicode.financetracker.admin;

import com.marakicode.financetracker.common.ApiResponse;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.transactions.dto.TransactionSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTransactionController {

    private final AdminTransactionService adminTransactionService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<TransactionSummary>>> listTransactions(
            @RequestParam(required = false) String search,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        PagedResponse<TransactionSummary> response =
                adminTransactionService.listTransactions(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionSummary>> getTransaction(
            @PathVariable Long id) {
        TransactionSummary response =
                adminTransactionService.getTransaction(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
