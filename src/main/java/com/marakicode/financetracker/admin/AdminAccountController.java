package com.marakicode.financetracker.admin;

import com.marakicode.financetracker.accounts.dto.AccountSummary;
import com.marakicode.financetracker.common.ApiResponse;
import com.marakicode.financetracker.common.PagedResponse;
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
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AccountSummary>>> listAccounts(
            @RequestParam(required = false) String search,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        PagedResponse<AccountSummary> response =
                adminAccountService.listAccounts(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountSummary>> getAccount(
            @PathVariable Long id) {
        AccountSummary response = adminAccountService.getAccount(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/freeze")
    public ResponseEntity<ApiResponse<Void>> freezeAccount(
            @PathVariable Long id) {
        adminAccountService.freezeAccount(id);
        return ResponseEntity.ok(
                ApiResponse.success("Account frozen successfully", null));
    }

    @PatchMapping("/{id}/unfreeze")
    public ResponseEntity<ApiResponse<Void>> unfreezeAccount(
            @PathVariable Long id) {
        adminAccountService.unfreezeAccount(id);
        return ResponseEntity.ok(
                ApiResponse.success("Account unfrozen successfully", null));
    }
}
