package com.example.middemo.dto.user;

import com.example.middemo.entity.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank(message = "must not be blank")
        @Size(min = 2, max = 50, message = "must be between 2 and 50 characters")
        String name,

        @NotBlank(message = "must not be blank")
        @Email(message = "must be a well-formed email address")
        @Size(max = 120, message = "must be at most 120 characters")
        String email,

        @NotNull(message = "must not be null")
        @Min(value = 1, message = "must be greater than or equal to 1")
        @Max(value = 150, message = "must be less than or equal to 150")
        Integer age,

        UserStatus status
) {
}
