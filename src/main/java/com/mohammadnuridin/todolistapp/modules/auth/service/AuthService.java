package com.mohammadnuridin.todolistapp.modules.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.mohammadnuridin.todolistapp.modules.auth.dto.AuthResponse;
import com.mohammadnuridin.todolistapp.modules.auth.dto.ChangePasswordRequest;
import com.mohammadnuridin.todolistapp.modules.auth.dto.LoginRequest;

public interface AuthService {

    // isWeb → service tahu apakah perlu buat session atau tidak
    AuthResponse login(LoginRequest request, boolean isWeb, HttpServletResponse response);

    // rawRefreshToken → sudah di-resolve oleh controller (dari Cookie atau body)
    AuthResponse refresh(String rawRefreshToken, boolean isWeb, HttpServletResponse response);

    // rawRefreshToken → sudah di-resolve oleh controller
    void logout(HttpServletRequest request, String rawRefreshToken, boolean isWeb,
            HttpServletResponse response);

    void changePassword(ChangePasswordRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse);
}