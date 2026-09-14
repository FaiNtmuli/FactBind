package com.example.middemo.controller;

import com.example.middemo.dto.common.PageResponse;
import com.example.middemo.dto.user.CreateUserRequest;
import com.example.middemo.dto.user.UpdateUserRequest;
import com.example.middemo.dto.user.UpdateUserStatusRequest;
import com.example.middemo.dto.user.UserResponse;
import com.example.middemo.entity.UserStatus;
import com.example.middemo.service.UserService;
import com.example.middemo.factbind.FactBind;
import com.example.middemo.factbind.FactBindParam;
import com.example.middemo.factbind.ContractRegistry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

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
    private final ContractRegistry contract;

    public UserController(UserService userService, ContractRegistry contract) {
        this.userService = userService;
        this.contract = contract;
    }

    /** {@code GET /api/users?keyword=tom&status=ACTIVE&page=0&size=20} */
    @FactBind("User.List")
    public PageResponse<UserResponse> listUsers(
            @FactBindParam String keyword,
            @FactBindParam UserStatus status,
            @FactBindParam @Min(0) int page,
            @FactBindParam @Min(1) @Max(100) int size
    ) {
        return userService.searchUsers(keyword, status, page, size);
    }

    /** {@code GET /api/users/{id}} */
    @FactBind("User.Get")
    public UserResponse getUser(@FactBindParam Long id) {
        return userService.getUser(id);
    }

    /** {@code POST /api/users} */
    @FactBind("User.Create")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = userService.createUser(request);
        return ResponseEntity
                .created(URI.create(contract.path("User.Get", Map.of("id", created.id()))))
                .body(created);
    }

    /** {@code PUT /api/users/{id}} */
    @FactBind("User.Update")
    public UserResponse updateUser(
            @FactBindParam Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return userService.updateUser(id, request);
    }

    /** {@code PATCH /api/users/{id}/status} */
    @FactBind("User.UpdateStatus")
    public UserResponse updateUserStatus(
            @FactBindParam Long id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return userService.updateUserStatus(id, request);
    }

    /** {@code DELETE /api/users/{id}} */
    @FactBind("User.Delete")
    public ResponseEntity<Void> deleteUser(@FactBindParam Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
