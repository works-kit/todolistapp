package com.mohammadnuridin.todolistapp.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @Size(min = 2, max = 100, message = "{user.name.size}") String name,

        @Email(message = "{user.email.invalid}") @Size(max = 150, message = "{user.email.size}") String email

) {

    public boolean isEmpty() {
        return (name == null || name.isBlank()) &&
                (email == null || email.isBlank());
    }
}