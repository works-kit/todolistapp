package com.mohammadnuridin.todolistapp.modules.user.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mohammadnuridin.todolistapp.modules.user.domain.Role;
import com.mohammadnuridin.todolistapp.modules.user.domain.User;

public record UserResponse(
        String id,
        String email,
        String name,
        Role role,
        @JsonProperty("is_active") boolean isActive,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
