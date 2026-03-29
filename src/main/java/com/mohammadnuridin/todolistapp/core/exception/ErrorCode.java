package com.mohammadnuridin.todolistapp.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ── Generic ──────────────────────────────────────────────
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ERR_000", "error.internal_server_error"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "ERR_001", "error.validation_failed"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_002", "error.not_found"),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "ERR_003", "error.duplicate_resource"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "ERR_004", "error.forbidden"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "ERR_005", "error.unauthorized"),

    // ── Auth ─────────────────────────────────────────────────
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_001", "error.auth.invalid_credentials"),
    ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "AUTH_002", "error.auth.account_disabled"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_003", "error.auth.token_expired"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_004", "error.auth.token_invalid"),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_005", "error.auth.refresh_token_invalid"),
    TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, "AUTH_006", "error.auth.token_blacklisted"),

    // ── User ─────────────────────────────────────────────────
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USR_001", "error.user.not_found"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "USR_002", "error.user.email_exists"),
    WRONG_PASSWORD(HttpStatus.BAD_REQUEST, "USR_003", "error.user.wrong_password"),

    // ── Todo ─────────────────────────────────────────────────
    TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "TODO_001", "error.todo.not_found"),
    TODO_ACCESS_DENIED(HttpStatus.FORBIDDEN, "TODO_002", "error.todo.access_denied"),

    // ── Category ─────────────────────────────────────────────
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CAT_001", "error.category.not_found"),
    CATEGORY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CAT_002", "error.category.access_denied"),
    CATEGORY_NAME_EXISTS(HttpStatus.CONFLICT, "CAT_003", "error.category.name_exists");

    private final HttpStatus httpStatus;
    private final String code;
    private final String messageKey; // key ke messages.properties

    ErrorCode(HttpStatus httpStatus, String code, String messageKey) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.messageKey = messageKey;
    }
}