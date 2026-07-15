package com.marakicode.financetracker.accounts;

import com.marakicode.financetracker.accounts.dto.AccountCreateRequest;
import com.marakicode.financetracker.accounts.dto.AccountResponse;
import com.marakicode.financetracker.accounts.dto.CurrencyUpdateRequest;
import com.marakicode.financetracker.accounts.dto.UpdateAccountTypeRequest;
import com.marakicode.financetracker.common.ApiResponse;
import com.marakicode.financetracker.common.PagedResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Create account", description = "Create a new financial account for the authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Account created successfully",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed")
    })
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody AccountCreateRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", response));
    }

    @GetMapping("/mine")
    @Operation(summary = "Get my accounts", description = "Retrieve a paginated list of accounts belonging to the authenticated user. Supports filtering by name, type, and currency.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paginated list of your accounts",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    })
    public ResponseEntity<ApiResponse<PagedResponse<AccountResponse>>> getMyAccounts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AccountType type,
            @RequestParam(required = false) String currency,
            @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        PagedResponse<AccountResponse> response = accountService.getAccounts(search, type, currency, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID", description = "Retrieve an account by its ID. Only the owning user can access their accounts.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account found",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — not the account owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(@PathVariable Long id) {
        AccountResponse response = accountService.getAccountById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List accounts", description = "Retrieve a paginated list of accounts for the authenticated user. Supports filtering by name, type, and currency.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paginated list of accounts",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<AccountResponse>>> getAccounts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AccountType type,
            @RequestParam(required = false) String currency,
            @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        PagedResponse<AccountResponse> response = accountService.getAccounts(search, type, currency, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update account currency", description = "Update the currency of an account. Only the owning user can update their accounts.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Currency updated successfully",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — not the account owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody CurrencyUpdateRequest request) {
        AccountResponse response = accountService.updateAccount(id, request);
        return ResponseEntity.ok(ApiResponse.success("Currency updated successfully", response));
    }

    @PatchMapping("/{id}/type")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update account type", description = "Update the type of an account (admin only).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account type updated successfully",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccountType(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAccountTypeRequest request) {
        AccountResponse response = accountService.updateAccountType(id, request);
        return ResponseEntity.ok(ApiResponse.success("Account type updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete account", description = "Delete an account by its ID. Only the owning user can delete their accounts.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Account deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — not the account owner"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
