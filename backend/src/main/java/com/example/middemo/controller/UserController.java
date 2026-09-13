package com.example.middemo.controller;

import com.example.middemo.dto.common.PageResponse;
import com.example.middemo.dto.user.CreateUserRequest;
import com.example.middemo.dto.user.UpdateUserRequest;
import com.example.middemo.dto.user.UpdateUserStatusRequest;
import com.example.middemo.dto.user.UserResponse;
import com.example.middemo.entity.UserStatus;
import com.example.middemo.service.UserService;
import com.example.middemo.web.ApiPaths;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * User endpoints.
 *
 * <p>Binding styles covered here: pure path parameter, multiple query parameters
 * (optional keyword / enum status, pagination) and JSON request bodies.
 */
@RestController
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** {@code GET /api/users?keyword=tom&status=ACTIVE&page=0&size=20} */
    @GetMapping(ApiPaths.USERS)
    public PageResponse<UserResponse> listUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) UserStatus status,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return userService.searchUsers(keyword, status, page, size);
    }

    /** {@code GET /api/users/{id}} */
    @GetMapping(ApiPaths.USER_BY_ID)
    public UserResponse getUser(@PathVariable("id") Long id) {
        return userService.getUser(id);
    }

    /** {@code POST /api/users} */
    @PostMapping(ApiPaths.USERS)
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = userService.createUser(request);
        return ResponseEntity
                .created(URI.create(ApiPaths.withId(ApiPaths.USER_BY_ID, created.id())))
                .body(created);
    }

    /** {@code PUT /api/users/{id}} */
    @PutMapping(ApiPaths.USER_BY_ID)
    public UserResponse updateUser(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return userService.updateUser(id, request);
    }

    /** {@code PATCH /api/users/{id}/status} */
    @PatchMapping(ApiPaths.USER_STATUS)
    public UserResponse updateUserStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return userService.updateUserStatus(id, request);
    }

    /** {@code DELETE /api/users/{id}} */
    @DeleteMapping(ApiPaths.USER_BY_ID)
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
