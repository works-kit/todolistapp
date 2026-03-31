package com.mohammadnuridin.todolistapp.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "{user.email.not_blank}") @Email(message = "{auth.email.invalid}") String email,

        @NotBlank(message = "{user.password.not_blank}") String password) {
}