package com.marakicode.financetracker.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marakicode.financetracker.common.DuplicateResourceException;
import com.marakicode.financetracker.common.PagedResponse;
import com.marakicode.financetracker.common.ResourceNotFoundException;
import com.marakicode.financetracker.users.dto.PasswordUpdateRequest;
import com.marakicode.financetracker.users.dto.UserCreateRequest;
import com.marakicode.financetracker.users.dto.UserDto;
import com.marakicode.financetracker.users.dto.UserUpdateRequest;
import com.marakicode.financetracker.users.exceptions.PasswordMismatchException;
import com.marakicode.financetracker.users.Role;
import com.marakicode.financetracker.users.exceptions.LastAdminActionException;
import com.marakicode.financetracker.auth.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    private static UserDto sampleUserDto() {
        return new UserDto(1L, "Alice", "Smith", "alice@example.com", Role.USER, LocalDateTime.of(2025, 1, 15, 10, 30));
    }

    @Test
    @DisplayName("createUser_shouldReturn201_withValidRequest - POST with valid body returns 201 with user data")
    void createUser_shouldReturn201_withValidRequest() throws Exception {

        // Arrange
        var request = new UserCreateRequest("Alice", "Smith", "alice@example.com", "Secret123!");
        var response = sampleUserDto();
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.data.firstName").value("Alice"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("createUser_shouldReturn400_withInvalidRequest - POST with missing fields returns 400 with field errors")
    void createUser_shouldReturn400_withInvalidRequest() throws Exception {

        // Arrange
        var invalidRequest = new UserCreateRequest("", "", "", "");

        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors.length()").isNumber())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'firstName')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'lastName')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").exists());
    }

    @Test
    @DisplayName("createUser_shouldReturn409_whenEmailExists - POST with duplicate email returns 409")
    void createUser_shouldReturn409_whenEmailExists() throws Exception {

        // Arrange
        var request = new UserCreateRequest("Alice", "Smith", "alice@example.com", "Secret123!");
        when(userService.createUser(any(UserCreateRequest.class)))
                .thenThrow(new DuplicateResourceException("Email already exists: alice@example.com"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Email already exists: alice@example.com"));
    }

    @Test
    @DisplayName("getUser_shouldReturn200_withValidId - GET with existing ID returns 200 with user")
    void getUser_shouldReturn200_withValidId() throws Exception {

        // Arrange
        var response = sampleUserDto();
        when(userService.getUserById(1L)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.firstName").value("Alice"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("getUser_shouldReturn404_whenNotFound - GET with nonexistent ID returns 404")
    void getUser_shouldReturn404_whenNotFound() throws Exception {

        // Arrange
        when(userService.getUserById(999L))
                .thenThrow(new ResourceNotFoundException("User not found with id: 999"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("getUsers_shouldReturn200_withPagedResponse - GET returns paginated user list")
    void getUsers_shouldReturn200_withPagedResponse() throws Exception {

        // Arrange
        var users = List.of(sampleUserDto());
        var pagedResponse = new PagedResponse<>(users, 0, 10, 1, 1);
        when(userService.getAllUsers(any())).thenReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    @DisplayName("updateUser_shouldReturn200_withValidRequest - PATCH with valid body returns 200 with updated user")
    void updateUser_shouldReturn200_withValidRequest() throws Exception {

        // Arrange
        var request = new UserUpdateRequest("Alice", "Johnson");
        var updatedResponse = new UserDto(1L, "Alice", "Johnson", "alice@example.com", Role.USER, LocalDateTime.of(2025, 1, 15, 10, 30));
        when(userService.updateUser(eq(1L), any(UserUpdateRequest.class))).thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User updated successfully"))
                .andExpect(jsonPath("$.data.firstName").value("Alice"))
                .andExpect(jsonPath("$.data.lastName").value("Johnson"));
    }

    @Test
    @DisplayName("updateUser_shouldReturn404_whenNotFound - PATCH with nonexistent ID returns 404")
    void updateUser_shouldReturn404_whenNotFound() throws Exception {

        // Arrange
        var request = new UserUpdateRequest("Alice", "Johnson");
        when(userService.updateUser(eq(999L), any(UserUpdateRequest.class)))
                .thenThrow(new ResourceNotFoundException("User not found with id: 999"));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/users/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("deleteUser_shouldReturn204_withValidId - DELETE with existing ID returns 204 No Content")
    void deleteUser_shouldReturn204_withValidId() throws Exception {

        // Arrange
        doNothing().when(userService).deleteUser(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("deleteUser_shouldReturn404_whenNotFound - DELETE with nonexistent ID returns 404")
    void deleteUser_shouldReturn404_whenNotFound() throws Exception {

        // Arrange
        doThrow(new ResourceNotFoundException("User not found with id: 999"))
                .when(userService).deleteUser(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with id: 999"));
    }

    @Test
    @DisplayName("updatePassword_shouldReturn200_withValidRequest - PATCH with valid password body returns 200")
    void updatePassword_shouldReturn200_withValidRequest() throws Exception {

        // Arrange
        var request = new PasswordUpdateRequest("OldPass123!", "NewPass456!");
        doNothing().when(userService).updatePassword(eq(1L), any(PasswordUpdateRequest.class));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/users/1/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password updated successfully"));
    }

    @Test
    @DisplayName("updatePassword_shouldReturn400_whenPasswordMismatch - PATCH with wrong old password returns 400")
    void updatePassword_shouldReturn400_whenPasswordMismatch() throws Exception {

        // Arrange
        var request = new PasswordUpdateRequest("WrongPass!", "NewPass456!");
        doThrow(new PasswordMismatchException("Current password is incorrect"))
                .when(userService).updatePassword(eq(1L), any(PasswordUpdateRequest.class));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/users/1/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Password Mismatch"))
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));
    }

    @Test
    @DisplayName("updatePassword_shouldReturn404_whenNotFound - PATCH with nonexistent ID returns 404")
    void updatePassword_shouldReturn404_whenNotFound() throws Exception {

        // Arrange
        var request = new PasswordUpdateRequest("OldPass123!", "NewPass456!");
        doThrow(new ResourceNotFoundException("User not found with id: 999"))
                .when(userService).updatePassword(eq(999L), any(PasswordUpdateRequest.class));

        // Act & Assert
        mockMvc.perform(patch("/api/v1/users/999/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with id: 999"));
    }

    @Test
    @DisplayName("updateUser_shouldReturn405_whenUsingPut - PUT is not allowed on users endpoint")
    void updateUser_shouldReturn405_whenUsingPut() throws Exception {

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Alice\",\"lastName\":\"Smith\"}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("deleteUser_shouldReturn409_whenLastAdmin - DELETE last admin returns 409 Conflict")
    void deleteUser_shouldReturn409_whenLastAdmin() throws Exception {

        // Arrange
        doThrow(new LastAdminActionException("Cannot delete the last admin user"))
                .when(userService).deleteUser(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Cannot delete the last admin user"));
    }
}
