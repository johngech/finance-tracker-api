package com.marakicode.financetracker.admin;

import com.marakicode.financetracker.accounts.dto.AccountSummary;
import com.marakicode.financetracker.common.ApiResponse;
import com.marakicode.financetracker.common.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Accounts", description = "Admin account management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @GetMapping
    @Operation(summary = "List all accounts", description = "Retrieve a paginated list of all accounts across all users (admin only).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paginated list of accounts",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role")
    })
    public ResponseEntity<ApiResponse<PagedResponse<AccountSummary>>> listAccounts(
            @RequestParam(required = false) String search,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        PagedResponse<AccountSummary> response =
                adminAccountService.listAccounts(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID", description = "Retrieve any account by its ID (admin only).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account found",
                    content = @Content(schema = @Schema(implementation = AccountSummary.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<ApiResponse<AccountSummary>> getAccount(
            @PathVariable Long id) {
        AccountSummary response = adminAccountService.getAccount(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/freeze")
    @Operation(summary = "Freeze account", description = "Freeze an account to prevent transactions (admin only).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account frozen successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<ApiResponse<Void>> freezeAccount(
            @PathVariable Long id) {
        adminAccountService.freezeAccount(id);
        return ResponseEntity.ok(
                ApiResponse.success("Account frozen successfully", null));
    }

    @PatchMapping("/{id}/unfreeze")
    @Operation(summary = "Unfreeze account", description = "Unfreeze a previously frozen account (admin only).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Account unfrozen successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<ApiResponse<Void>> unfreezeAccount(
            @PathVariable Long id) {
        adminAccountService.unfreezeAccount(id);
        return ResponseEntity.ok(
                ApiResponse.success("Account unfrozen successfully", null));
    }
}
