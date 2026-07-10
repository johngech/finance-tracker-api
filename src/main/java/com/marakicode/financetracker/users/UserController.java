package com.marakicode.financetracker.users;

import com.marakicode.financetracker.common.ApiResponse;
import com.marakicode.financetracker.common.ErrorDto;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.users.exceptions.PasswordMismatchException;
import jakarta.servlet.http.HttpServletRequest;
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
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserDto userResponse = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", userResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable Long id) {
        UserDto userResponse = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> getUsers(
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        PagedResponse<UserDto> pagedResponse = userService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(pagedResponse));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        UserDto userResponse = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", userResponse));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/change-password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @PathVariable Long id,
            @Valid @RequestBody PasswordUpdateRequest request) {
        userService.updatePassword(id, request);
        return ResponseEntity.ok(ApiResponse.success("Password updated successfully", null));
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorDto> handlePasswordMismatchException(
            PasswordMismatchException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorDto.of(
                        HttpStatus.BAD_REQUEST.value(),
                        "Password Mismatch",
                        ex.getMessage(),
                        request.getRequestURI()));
    }
}
