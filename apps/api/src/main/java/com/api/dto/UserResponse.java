package com.api.dto;

import com.api.model.User;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String defaultCurrency,
        String timezone
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getDefaultCurrency(),
                user.getTimezone()
        );
    }
}
