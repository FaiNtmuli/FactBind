package com.example.middemo.service;

import com.example.middemo.dto.common.PageResponse;
import com.example.middemo.dto.user.CreateUserRequest;
import com.example.middemo.dto.user.UpdateUserRequest;
import com.example.middemo.dto.user.UpdateUserStatusRequest;
import com.example.middemo.dto.user.UserResponse;
import com.example.middemo.entity.User;
import com.example.middemo.entity.UserStatus;
import com.example.middemo.exception.DuplicateEmailException;
import com.example.middemo.exception.UserHasOrdersException;
import com.example.middemo.exception.UserNotFoundException;
import com.example.middemo.repository.OrderRepository;
import com.example.middemo.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public UserService(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchUsers(String keyword, UserStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Specification<User> spec = Specification.allOf(
                UserRepository.hasKeyword(keyword),
                UserRepository.hasStatus(status)
        );
        Page<User> result = userRepository.findAll(spec, pageable);
        return PageResponse.from(result, UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return UserResponse.from(requireUser(id));
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        User user = new User(request.name(), request.email(), request.age(), request.status());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = requireUser(id);
        if (!user.getEmail().equalsIgnoreCase(request.email())
                && userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        user.setName(request.name());
        user.setEmail(request.email());
        user.setAge(request.age());
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
        User user = requireUser(id);
        user.setStatus(request.status());
        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = requireUser(id);
        if (orderRepository.existsByUserId(user.getId())) {
            throw new UserHasOrdersException(user.getId());
        }
        userRepository.delete(user);
    }

    private User requireUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}
