package com.example.middemo.dto.user;

import com.example.middemo.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(

        @NotNull(message = "must not be null")
        UserStatus status
) {
}
