package com.mohammadnuridin.todolistapp.core.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Log auth events dari Spring Security secara otomatis.
 *
 * Login success / failure di-emit oleh AuthenticationManager
 * tanpa perlu tambah kode di AuthService.
 *
 * Auth events lain (register, logout, refresh) di-log langsung
 * di AuthServiceImpl menggunakan log.info() biasa + MDC context.
 */
@Slf4j
@Component
public class AuthEventLogger {

    /**
     * Login berhasil — di-emit oleh Spring Security AuthenticationManager.
     */
    @EventListener
    public void onLoginSuccess(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        String email = (principal instanceof UserDetails ud)
                ? ud.getUsername()
                : principal.toString();

        log.info("AUTH_LOGIN_SUCCESS email={}", email);
    }

    /**
     * Login gagal — bad credentials, account disabled, dll.
     */
    @EventListener
    public void onLoginFailure(AbstractAuthenticationFailureEvent event) {
        String email = event.getAuthentication().getName();
        String reason = event.getException().getClass().getSimpleName();

        log.warn("AUTH_LOGIN_FAILURE email={} reason={}", email, reason);
    }
}
