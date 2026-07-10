package com.marakicode.financetracker.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marakicode.financetracker.common.DuplicateResourceException;
import com.marakicode.financetracker.users.UserDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private com.marakicode.financetracker.users.UserService userService;

    @Test
    @DisplayName("login should return 200 with access token when valid credentials are provided")
    void login_shouldReturn200_whenValidCredentials() throws Exception {

        // Arrange
        var request = new LoginRequest("alice@example.com", "Secret123!");
        var jwtResponse = new JwtResponse("access-token");
        when(authService.login(any(LoginRequest.class), any())).thenReturn(jwtResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    @DisplayName("login should return 400 when required fields are missing")
    void login_shouldReturn400_whenMissingFields() throws Exception {

        // Arrange
        var invalidRequest = new LoginRequest("", "");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").exists());
    }

    @Test
    @DisplayName("login should return 401 when bad credentials are provided")
    void login_shouldReturn401_whenBadCredentials() throws Exception {

        // Arrange
        var request = new LoginRequest("alice@example.com", "WrongPassword!");
        when(authService.login(any(LoginRequest.class), any()))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("register should return 201 with user data when valid request is provided")
    void register_shouldReturn201_whenValidRequest() throws Exception {

        // Arrange
        var request = new RegisterRequest("Alice", "Smith", "alice@example.com", "Secret123!");
        var userDto = new UserDto(1L, "Alice", "Smith", "alice@example.com", LocalDateTime.of(2025, 1, 15, 10, 30));
        when(authService.register(any(RegisterRequest.class), any())).thenReturn(userDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.data.firstName").value("Alice"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("register should return 409 when email is already registered")
    void register_shouldReturn409_whenDuplicateEmail() throws Exception {

        // Arrange
        var request = new RegisterRequest("Alice", "Smith", "alice@example.com", "Secret123!");
        when(authService.register(any(RegisterRequest.class), any()))
                .thenThrow(new DuplicateResourceException("Email already registered"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    @DisplayName("refresh should return 200 with new access token when valid refresh token cookie is provided")
    void refresh_shouldReturn200_whenValidCookie() throws Exception {

        // Arrange
        var jwtResponse = new JwtResponse("new-access-token");
        when(authService.refresh("valid.refresh.token")).thenReturn(jwtResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "valid.refresh.token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
    }

    @Test
    @DisplayName("refresh should return 401 when no refresh token cookie is present")
    void refresh_shouldReturn401_whenNoCookie() throws Exception {

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Refresh token not found in cookie"));
    }

    @Test
    @DisplayName("refresh should return 401 when refresh token is invalid")
    void refresh_shouldReturn401_whenInvalidToken() throws Exception {

        // Arrange
        when(authService.refresh("invalid.refresh.token"))
                .thenThrow(new InvalidJwtAuthenticationException("Invalid refresh token"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refreshToken", "invalid.refresh.token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
    }

    @Test
    @DisplayName("me should return 200 with user data when authenticated")
    @WithMockUser(username = "alice@example.com")
    void me_shouldReturn200_whenAuthenticated() throws Exception {

        // Arrange
        var userDto = new UserDto(1L, "Alice", "Smith", "alice@example.com", LocalDateTime.of(2025, 1, 15, 10, 30));
        when(authService.me()).thenReturn(userDto);

        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Alice"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"));
    }
}
