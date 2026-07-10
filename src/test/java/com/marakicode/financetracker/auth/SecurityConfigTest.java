package com.marakicode.financetracker.auth;

import com.marakicode.financetracker.users.User;
import com.marakicode.financetracker.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityConfigTest {

    private static final String TEST_EMAIL = "jwtuser@example.com";
    private static final String TEST_PASSWORD = "Secret123!";
    private static final String ADMIN_EMAIL = "admin@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String validBearerToken;
    private String adminBearerToken;

    @BeforeEach
    void setUp() {
        // Create a regular USER
        User user = new User();
        user.setFirstName("Jwt");
        user.setLastName("User");
        user.setEmail(TEST_EMAIL);
        user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        user.setRole(com.marakicode.financetracker.users.Role.USER);
        User savedUser = userRepository.save(user);
        validBearerToken = jwtService.generateAccessToken(savedUser).toString();

        // Create an ADMIN user
        User admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail(ADMIN_EMAIL);
        admin.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        admin.setRole(com.marakicode.financetracker.users.Role.ADMIN);
        User savedAdmin = userRepository.save(admin);
        adminBearerToken = jwtService.generateAccessToken(savedAdmin).toString();
    }

    // ── Unauthenticated access tests (401 Unauthorized) ────────────────

    @Test
    @DisplayName("POST /api/v1/users should return 401 when unauthenticated")
    void postUsers_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content("{\"firstName\":\"Alice\",\"lastName\":\"Smith\",\"email\":\"alice@example.com\",\"password\":\"Secret123!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("GET /api/v1/users should return 401 when unauthenticated")
    void getUsers_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} should return 401 when unauthenticated")
    void getUserById_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id} should return 401 when unauthenticated")
    void updateUser_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(patch("/api/v1/users/1")
                        .contentType("application/json")
                        .content("{\"firstName\":\"Alice\",\"lastName\":\"Smith\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} should return 401 when unauthenticated")
    void deleteUser_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id}/change-password should return 401 when unauthenticated")
    void changePassword_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(patch("/api/v1/users/1/change-password")
                        .contentType("application/json")
                        .content("{\"oldPassword\":\"OldPass123!\",\"newPassword\":\"NewPass456!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    // ── POST /api/v1/users admin-only restriction ────────────────────

    @Test
    @DisplayName("POST /api/v1/users should deny regular USER (403)")
    void postUsers_shouldDeny_whenRegularUser() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + validBearerToken)
                        .contentType("application/json")
                        .content("{\"firstName\":\"Alice\",\"lastName\":\"Smith\",\"email\":\"alice@example.com\",\"password\":\"Secret123!\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/users should allow ADMIN user (201)")
    void postUsers_shouldAllow_whenAdminUser() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminBearerToken)
                        .contentType("application/json")
                        .content("{\"firstName\":\"Alice\",\"lastName\":\"Smith\",\"email\":\"alice@example.com\",\"password\":\"Secret123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Alice"));
    }

    // ── JWT-authenticated access tests ─────────────────────────────

    @Test
    @DisplayName("GET /api/v1/users should return 200 when valid JWT is provided")
    void getUsers_shouldReturn200_whenValidJwt() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + validBearerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 200 with access token and set refresh token cookie when valid credentials are provided")
    void login_shouldReturn200_whenValidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + TEST_EMAIL + "\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/api/v1/auth"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 201 with user data and set refresh token cookie when valid request is provided")
    void register_shouldReturn201_whenValidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("{\"firstName\":\"New\",\"lastName\":\"User\",\"email\":\"newuser@example.com\",\"password\":\"Secret123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("New"))
                .andExpect(jsonPath("$.data.lastName").value("User"))
                .andExpect(jsonPath("$.data.email").value("newuser@example.com"))
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/api/v1/auth"));
    }
}
