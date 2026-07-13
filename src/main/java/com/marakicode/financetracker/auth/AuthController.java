package com.marakicode.financetracker.auth;

import com.marakicode.financetracker.common.ApiResponse;
import com.marakicode.financetracker.users.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Login with email and password to receive JWT access token. A refresh token is set as an HttpOnly cookie.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    public ResponseEntity<ApiResponse<JwtResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        var jwtResponse = authService.login(request, response);
        return ResponseEntity.ok(ApiResponse.success("Login successful", jwtResponse));
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Create a new user account. A refresh token is set as an HttpOnly cookie.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Registration successful",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed or email already in use")
    })
    public ResponseEntity<ApiResponse<UserDto>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        var userDto = authService.register(request, response);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", userDto));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns the profile of the currently authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Current user profile",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<ApiResponse<UserDto>> me() {
        var userDto = authService.me();
        return ResponseEntity.ok(ApiResponse.success(userDto));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Exchange a valid refresh token (from cookie) for a new access token.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid or missing refresh token")
    })
    public ResponseEntity<ApiResponse<JwtResponse>> refresh(
            @CookieValue(value = "refreshToken", required = false) String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidJwtAuthenticationException("Refresh token not found in cookie");
        }
        var jwtResponse = authService.refresh(token);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", jwtResponse));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Clear the refresh token cookie and invalidate the session.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        authService.logout(response);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
}
