package com.example.middemo.service;

import com.example.middemo.dto.common.PageResponse;
import com.example.middemo.dto.user.CreateUserRequest;
import com.example.middemo.dto.user.UpdateUserRequest;
import com.example.middemo.dto.user.UpdateUserStatusRequest;
import com.example.middemo.dto.user.UserResponse;
import com.example.middemo.entity.UserStatus;
import com.example.middemo.exception.DuplicateEmailException;
import com.example.middemo.exception.UserHasOrdersException;
import com.example.middemo.exception.UserNotFoundException;
import com.example.middemo.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Service level tests for the user module, running against the real H2 database.
 *
 * <p>These tests are not transactional on purpose: the business rules under test (for example
 * the delete guard) must be verified against committed data.
 */
@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    @DisplayName("createUser stores the user with status ACTIVE by default")
    void createUserUsesDefaultStatus() {
        UserResponse created = userService.createUser(new CreateUserRequest("Nina Novak", uniqueEmail(), 33, null));

        assertThat(created.id()).isNotNull();
        assertThat(created.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(created.createdAt()).isNotNull();
        assertThat(userRepository.findById(created.id())).isPresent();
    }

    @Test
    @DisplayName("createUser rejects a duplicated email")
    void createUserRejectsDuplicateEmail() {
        String email = uniqueEmail();
        userService.createUser(new CreateUserRequest("First User", email, 30, null));

        assertThatThrownBy(() -> userService.createUser(new CreateUserRequest("Second User", email, 31, null)))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining(email);
    }

    @Test
    @DisplayName("updateUser changes name, email and age")
    void updateUser() {
        UserResponse created = userService.createUser(new CreateUserRequest("Old Name", uniqueEmail(), 30, null));
        String newEmail = uniqueEmail();

        UserResponse updated = userService.updateUser(created.id(), new UpdateUserRequest("New Name", newEmail, 41));

        assertThat(updated.name()).isEqualTo("New Name");
        assertThat(updated.email()).isEqualTo(newEmail);
        assertThat(updated.age()).isEqualTo(41);
    }

    @Test
    @DisplayName("updateUser rejects an email that belongs to somebody else")
    void updateUserRejectsDuplicateEmail() {
        userService.createUser(new CreateUserRequest("Other User", uniqueEmail(), 30, null));
        String takenEmail = uniqueEmail();
        userService.createUser(new CreateUserRequest("Owner User", takenEmail, 30, null));
        UserResponse target = userService.createUser(new CreateUserRequest("Target User", uniqueEmail(), 30, null));

        assertThatThrownBy(() -> userService.updateUser(target.id(), new UpdateUserRequest("Target User", takenEmail, 30)))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    @DisplayName("updateUserStatus disables and re-enables a user")
    void updateUserStatus() {
        UserResponse created = userService.createUser(new CreateUserRequest("Status User", uniqueEmail(), 30, null));

        UserResponse disabled = userService.updateUserStatus(created.id(), new UpdateUserStatusRequest(UserStatus.DISABLED));
        assertThat(disabled.status()).isEqualTo(UserStatus.DISABLED);

        UserResponse active = userService.updateUserStatus(created.id(), new UpdateUserStatusRequest(UserStatus.ACTIVE));
        assertThat(active.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("getUser rejects an unknown id")
    void getUserRejectsUnknownId() {
        assertThatThrownBy(() -> userService.getUser(999_999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User 999999 not found");
    }

    @Test
    @DisplayName("searchUsers filters by keyword and status")
    void searchUsersFilters() {
        String token = "zzq" + UUID.randomUUID().toString().substring(0, 8);
        userService.createUser(new CreateUserRequest("Keyword " + token, uniqueEmail(), 30, null));
        UserResponse disabled = userService.createUser(new CreateUserRequest("Disabled " + token, uniqueEmail(), 30, null));
        userService.updateUserStatus(disabled.id(), new UpdateUserStatusRequest(UserStatus.DISABLED));

        PageResponse<UserResponse> all = userService.searchUsers(token, null, 0, 20);
        assertThat(all.totalElements()).isEqualTo(2);

        PageResponse<UserResponse> activeOnly = userService.searchUsers(token, UserStatus.ACTIVE, 0, 20);
        assertThat(activeOnly.content()).hasSize(1);
        assertThat(activeOnly.content().getFirst().status()).isEqualTo(UserStatus.ACTIVE);

        PageResponse<UserResponse> disabledOnly = userService.searchUsers(token, UserStatus.DISABLED, 0, 20);
        assertThat(disabledOnly.content()).hasSize(1);
        assertThat(disabledOnly.content().getFirst().id()).isEqualTo(disabled.id());
    }

    @Test
    @DisplayName("searchUsers paginates the result")
    void searchUsersPaginates() {
        String token = "zpg" + UUID.randomUUID().toString().substring(0, 8);
        for (int i = 0; i < 3; i++) {
            userService.createUser(new CreateUserRequest("Paged " + token + " " + i, uniqueEmail(), 30, null));
        }

        PageResponse<UserResponse> firstPage = userService.searchUsers(token, null, 0, 2);
        assertThat(firstPage.content()).hasSize(2);
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.first()).isTrue();
        assertThat(firstPage.last()).isFalse();

        PageResponse<UserResponse> secondPage = userService.searchUsers(token, null, 1, 2);
        assertThat(secondPage.content()).hasSize(1);
        assertThat(secondPage.last()).isTrue();
    }

    @Test
    @DisplayName("deleteUser removes an unused user")
    void deleteUser() {
        UserResponse created = userService.createUser(new CreateUserRequest("Deletable User", uniqueEmail(), 30, null));

        userService.deleteUser(created.id());

        assertThat(userRepository.findById(created.id())).isEmpty();
    }

    @Test
    @DisplayName("deleteUser is refused while the user still has orders")
    void deleteUserWithOrdersIsRefused() {
        UserResponse user = userService.createUser(new CreateUserRequest("Ordering User", uniqueEmail(), 30, null));
        ProductResponseHolder product = createProduct("SKU-" + UUID.randomUUID());
        orderService.createOrder(new com.example.middemo.dto.order.CreateOrderRequest(
                user.id(),
                "keeps the user alive",
                java.util.List.of(new com.example.middemo.dto.order.CreateOrderItemRequest(product.id(), 1))
        ));

        assertThatThrownBy(() -> userService.deleteUser(user.id()))
                .isInstanceOf(UserHasOrdersException.class);
        assertThat(userRepository.findById(user.id())).isPresent();
    }

    private ProductResponseHolder createProduct(String sku) {
        var created = productService.createProduct(new com.example.middemo.dto.product.CreateProductRequest(
                "Test Product", sku, new java.math.BigDecimal("10.00"), 10, null));
        return new ProductResponseHolder(created.id());
    }

    private record ProductResponseHolder(Long id) {
    }
}
