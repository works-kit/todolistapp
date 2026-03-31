package com.mohammadnuridin.todolistapp.modules.user;

import com.mohammadnuridin.todolistapp.modules.user.dto.UpdateProfileRequest;
import com.mohammadnuridin.todolistapp.modules.user.dto.UserResponse;

public interface UserService {

    UserResponse getProfile();

    UserResponse updateProfile(UpdateProfileRequest request);

    void deleteAccount();
}
