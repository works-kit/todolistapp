package com.mohammadnuridin.todolistapp.core.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mohammadnuridin.todolistapp.core.exception.AppException;
import com.mohammadnuridin.todolistapp.core.exception.ErrorCode;
import com.mohammadnuridin.todolistapp.modules.user.domain.UserDetailsImpl;

public final class SecurityUtil {

    private SecurityUtil() {
    }

    /**
     * Ambil UserDetailsImpl dari SecurityContext.
     * Throw UNAUTHORIZED jika tidak ada authentication.
     */
    public static UserDetailsImpl getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() ||
                !(auth.getPrincipal() instanceof UserDetailsImpl)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return (UserDetailsImpl) auth.getPrincipal();
    }

    /**
     * Ambil ID user yang sedang login.
     */
    public static String getCurrentUserId() {
        return getCurrentUser().getId();
    }

    /**
     * Ambil email user yang sedang login.
     */
    public static String getCurrentUserEmail() {
        return getCurrentUser().getEmail();
    }
}