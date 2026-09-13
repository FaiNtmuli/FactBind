package com.example.middemo.dto.user;

import com.example.middemo.entity.User;
import com.example.middemo.entity.UserStatus;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        Integer age,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAge(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
