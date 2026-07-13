package com.marakicode.financetracker.transactions;

import com.marakicode.financetracker.common.ApiResponse;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.transactions.dto.TransactionCreateRequest;
import com.marakicode.financetracker.transactions.dto.TransactionResponse;
import com.marakicode.financetracker.transactions.dto.TransactionUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @Operation(summary = "Create transaction", description = "Create a new transaction. INCOME adds to the account balance; EXPENSE subtracts.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Transaction created successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or insufficient funds"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(
            @Valid @RequestBody TransactionCreateRequest request) {
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction created successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID", description = "Retrieve a transaction by its ID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction found",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(@PathVariable Long id) {
        TransactionResponse response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List transactions", description = "Retrieve a paginated list of transactions. Supports filtering by account, type, category, text search, and date range.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paginated list of transactions",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    })
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> getTransactions(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(page = 0, size = 10, sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        PagedResponse<TransactionResponse> pagedResponse =
                transactionService.getTransactions(accountId, type, category, search, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success(pagedResponse));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update transaction", description = "Partially update a transaction. Account assignment is immutable after creation.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction updated successfully",
                    content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or insufficient funds"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public ResponseEntity<ApiResponse<TransactionResponse>> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionUpdateRequest request) {
        TransactionResponse response = transactionService.updateTransaction(id, request);
        return ResponseEntity.ok(ApiResponse.success("Transaction updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete transaction", description = "Delete a transaction. The account balance is reversed accordingly.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Transaction deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}
