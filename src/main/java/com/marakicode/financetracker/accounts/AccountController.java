package com.marakicode.financetracker.accounts;

import com.marakicode.financetracker.accounts.dto.AccountCreateRequest;
import com.marakicode.financetracker.accounts.dto.AccountResponse;
import com.marakicode.financetracker.accounts.dto.CurrencyUpdateRequest;
import com.marakicode.financetracker.accounts.dto.UpdateAccountTypeRequest;
import com.marakicode.financetracker.common.ApiResponse;
import com.marakicode.financetracker.common.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody AccountCreateRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(@PathVariable Long id) {
        AccountResponse response = accountService.getAccountById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AccountResponse>>> getAccounts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AccountType type,
            @RequestParam(required = false) String currency,
            @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        PagedResponse<AccountResponse> response = accountService.getAccounts(search, type, currency, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody CurrencyUpdateRequest request) {
        AccountResponse response = accountService.updateAccount(id, request);
        return ResponseEntity.ok(ApiResponse.success("Account updated successfully", response));
    }

    @PatchMapping("/{id}/type")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccountType(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAccountTypeRequest request) {
        AccountResponse response = accountService.updateAccountType(id, request);
        return ResponseEntity.ok(ApiResponse.success("Account type updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
