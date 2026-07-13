package com.marakicode.financetracker.users;

import com.marakicode.financetracker.common.ApiResponse;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.users.dto.PasswordUpdateRequest;
import com.marakicode.financetracker.users.dto.UserCreateRequest;
import com.marakicode.financetracker.users.dto.UserDto;
import com.marakicode.financetracker.users.dto.UserUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user", description = "Create a new user (admin only).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User created successfully",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or email already in use"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role")
    })
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserDto userResponse = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", userResponse));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id()")
    @Operation(summary = "Get user by ID", description = "Retrieve a user by their ID. Users can only access their own profile; admins can access any.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — not authorized to view this user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable Long id) {
        UserDto userResponse = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List users", description = "Retrieve a paginated list of all users (admin only).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paginated list of users",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — requires ADMIN role")
    })
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> getUsers(
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        PagedResponse<UserDto> pagedResponse = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(pagedResponse));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id()")
    @Operation(summary = "Update user", description = "Update a user's first and last name. Users can only update their own profile; admins can update any.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User updated successfully",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — not authorized to update this user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        UserDto userResponse = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", userResponse));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id()")
    @Operation(summary = "Delete user", description = "Delete a user by ID. Users can only delete their own profile; admins can delete any.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — not authorized to delete this user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/change-password")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id()")
    @Operation(summary = "Change password", description = "Change a user's password. Requires the current password for verification.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Password updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Current password is incorrect or validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden — not authorized to change this user's password"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @PathVariable Long id,
            @Valid @RequestBody PasswordUpdateRequest request) {
        userService.updatePassword(id, request);
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully", null));
    }
}
