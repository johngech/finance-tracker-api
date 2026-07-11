package com.marakicode.financetracker.admin;

import com.marakicode.financetracker.admin.dto.PasswordResetResponse;
import com.marakicode.financetracker.admin.dto.RoleUpdateRequest;
import com.marakicode.financetracker.common.ApiResponse;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.users.dto.UserSummary;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<UserSummary>>> listUsers(
            @RequestParam(required = false) String search,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        PagedResponse<UserSummary> response =
                adminUserService.listUsers(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserSummary>> getUser(
            @PathVariable Long id) {
        UserSummary response = adminUserService.getUser(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @PathVariable Long id) {
        adminUserService.suspendUser(id);
        return ResponseEntity.ok(
                ApiResponse.success("User suspended successfully", null));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<Void>> activateUser(
            @PathVariable Long id) {
        adminUserService.activateUser(id);
        return ResponseEntity.ok(
                ApiResponse.success("User activated successfully", null));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<Void>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request) {
        adminUserService.updateRole(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("User role updated successfully", null));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<PasswordResetResponse>> resetPassword(
            @PathVariable Long id) {
        PasswordResetResponse response = adminUserService.resetPassword(id);
        return ResponseEntity.ok(
                ApiResponse.success("Password reset successfully", response));
    }
}
