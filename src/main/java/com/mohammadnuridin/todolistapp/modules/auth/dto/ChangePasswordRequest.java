package com.mohammadnuridin.todolistapp.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

                @NotBlank(message = "{user.password.not_blank}") @JsonProperty("current_password") String currentPassword,

                @NotBlank(message = "{user.password.not_blank}") @Size(min = 8, max = 72, message = "{user.password.size}") @JsonProperty("new_password") String newPassword) {
}