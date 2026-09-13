package com.example.middemo.controller;

import com.example.middemo.dto.common.PageResponse;
import com.example.middemo.dto.user.UserResponse;
import com.example.middemo.entity.UserStatus;
import com.example.middemo.exception.DuplicateEmailException;
import com.example.middemo.exception.UserNotFoundException;
import com.example.middemo.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP binding, validation and status code tests for the user endpoints.
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2024-01-01T10:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private UserResponse sampleUser(Long id, UserStatus status) {
        return new UserResponse(id, "Alice Anderson", "alice@example.com", 30, status, CREATED_AT, CREATED_AT);
    }

    @Test
    @DisplayName("GET /api/users uses page=0 and size=20 when no parameter is given")
    void listUsersUsesDefaultPaging() throws Exception {
        given(userService.searchUsers(null, null, 0, 20))
                .willReturn(new PageResponse<>(List.of(sampleUser(1L, UserStatus.ACTIVE)), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].email").value("alice@example.com"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.first").value(true));

        verify(userService).searchUsers(null, null, 0, 20);
    }

    @Test
    @DisplayName("GET /api/users binds keyword, enum status and pagination")
    void listUsersBindsEveryQueryParameter() throws Exception {
        given(userService.searchUsers("tom", UserStatus.ACTIVE, 2, 5))
                .willReturn(new PageResponse<>(List.of(sampleUser(7L, UserStatus.ACTIVE)), 2, 5, 6, 2, false, true));

        mockMvc.perform(get("/api/users")
                        .param("keyword", "tom")
                        .param("status", "ACTIVE")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5));

        verify(userService).searchUsers("tom", UserStatus.ACTIVE, 2, 5);
    }

    @Test
    @DisplayName("GET /api/users returns 400 when the status enum is unknown")
    void listUsersRejectsUnknownStatusEnum() throws Exception {
        mockMvc.perform(get("/api/users").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    @DisplayName("GET /api/users returns 400 when size is out of range")
    void listUsersRejectsSizeOutOfRange() throws Exception {
        mockMvc.perform(get("/api/users").param("size", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PATCH /api/users returns 405 instead of falling through to 500")
    void methodNotAllowedIsNotAServerError() throws Exception {
        mockMvc.perform(patch("/api/users"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("POST /api/users with an unreadable Content-Type returns 415")
    void unsupportedMediaTypeIsNotAServerError() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<user/>"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    @DisplayName("GET /api/users/{id} returns the user")
    void getUserById() throws Exception {
        given(userService.getUser(1L)).willReturn(sampleUser(1L, UserStatus.ACTIVE));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Anderson"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/users/{id} returns 404 with the business error code")
    void getUserReturnsNotFound() throws Exception {
        given(userService.getUser(99L)).willThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("User 99 not found"));
    }

    @Test
    @DisplayName("POST /api/users returns 201 and a Location header")
    void createUserReturnsCreated() throws Exception {
        given(userService.createUser(any())).willReturn(sampleUser(21L, UserStatus.ACTIVE));

        String body = """
                {"name":"Alice Anderson","email":"alice@example.com","age":30}
                """;

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/21"))
                .andExpect(jsonPath("$.id").value(21));
    }

    @Test
    @DisplayName("POST /api/users returns 400 with a field map when the body is invalid")
    void createUserReturnsValidationErrors() throws Exception {
        String body = """
                {"name":"A","email":"not-an-email","age":0}
                """;

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fields.name").exists())
                .andExpect(jsonPath("$.fields.email").value("must be a well-formed email address"))
                .andExpect(jsonPath("$.fields.age").exists());
    }

    @Test
    @DisplayName("POST /api/users returns 400 when the body is missing")
    void createUserRejectsMissingBody() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"));
    }

    @Test
    @DisplayName("POST /api/users returns 409 for a duplicated email")
    void createUserReturnsConflictForDuplicateEmail() throws Exception {
        given(userService.createUser(any())).willThrow(new DuplicateEmailException("alice@example.com"));

        String body = """
                {"name":"Alice Anderson","email":"alice@example.com","age":30}
                """;

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }

    @Test
    @DisplayName("PUT /api/users/{id} updates an existing user")
    void updateUser() throws Exception {
        given(userService.updateUser(eq(1L), any())).willReturn(sampleUser(1L, UserStatus.ACTIVE));

        String body = """
                {"name":"Alice Anderson","email":"alice@example.com","age":31}
                """;

        mockMvc.perform(put("/api/users/1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PATCH /api/users/{id}/status disables a user")
    void updateUserStatus() throws Exception {
        given(userService.updateUserStatus(eq(1L), any())).willReturn(sampleUser(1L, UserStatus.DISABLED));

        mockMvc.perform(patch("/api/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    @DisplayName("PATCH /api/users/{id}/status returns 400 for a bad enum value")
    void updateUserStatusRejectsUnknownEnum() throws Exception {
        mockMvc.perform(patch("/api/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SLEEPING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST_BODY"));
    }

    @Test
    @DisplayName("DELETE /api/users/{id} returns 204 without a body")
    void deleteUserReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    @DisplayName("DELETE /api/users/{id} returns 409 when the user still has orders")
    void deleteUserReturnsConflictWhenOrdersExist() throws Exception {
        willThrow(new com.example.middemo.exception.UserHasOrdersException(1L))
                .given(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_HAS_ORDERS"));
    }
}
