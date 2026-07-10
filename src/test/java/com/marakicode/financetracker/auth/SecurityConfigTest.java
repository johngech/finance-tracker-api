package com.marakicode.financetracker.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/v1/users should permit unauthenticated requests and return user data")
    void postUsers_shouldPermit_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content("{\"firstName\":\"Alice\",\"lastName\":\"Smith\",\"email\":\"alice@example.com\",\"password\":\"Secret123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Alice"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("GET /api/v1/users should deny unauthenticated requests")
    void getUsers_shouldDeny_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} should deny unauthenticated requests")
    void getUserById_shouldDeny_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id} should deny unauthenticated requests")
    void updateUser_shouldDeny_whenUnauthenticated() throws Exception {
        mockMvc.perform(patch("/api/v1/users/1")
                        .contentType("application/json")
                        .content("{\"firstName\":\"Alice\",\"lastName\":\"Smith\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} should deny unauthenticated requests")
    void deleteUser_shouldDeny_whenUnauthenticated() throws Exception {
        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{id}/change-password should deny unauthenticated requests")
    void changePassword_shouldDeny_whenUnauthenticated() throws Exception {
        mockMvc.perform(patch("/api/v1/users/1/change-password")
                        .contentType("application/json")
                        .content("{\"oldPassword\":\"OldPass123!\",\"newPassword\":\"NewPass456!\"}"))
                .andExpect(status().isForbidden());
    }
}
