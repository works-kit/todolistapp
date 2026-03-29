package com.mohammadnuridin.todolistapp.core.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import com.mohammadnuridin.todolistapp.core.response.ApiResponse;
import com.mohammadnuridin.todolistapp.core.util.MessageService;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService msg;

    // ── 1. AppException (domain errors) ───────────────────────
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        String message = msg.get(ex.getErrorCode().getMessageKey());
        log.warn("AppException [{}]: {}", ex.getErrorCode().getCode(), message);
        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(ApiResponse.error(message));
    }

    // ── 2. Validation errors (@Valid on @RequestBody) ──────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        log.warn("Validation failed: {}", fieldErrors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        msg.get(ErrorCode.VALIDATION_ERROR.getMessageKey()),
                        fieldErrors));
    }

    // ── 3. Validation errors (@Validated on @RequestParam / @PathVariable) ──
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(
            HandlerMethodValidationException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getAllValidationResults().forEach(result -> result.getResolvableErrors().forEach(error -> {
            String field = result.getMethodParameter().getParameterName();
            if (field == null) {
                field = result.getMethodParameter().getParameterType().getSimpleName();
            }
            fieldErrors.put(field, error.getDefaultMessage());
        }));

        log.warn("Handler method validation failed: {}", fieldErrors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        msg.get(ErrorCode.VALIDATION_ERROR.getMessageKey()),
                        fieldErrors));
    }

    // ── 4. Constraint violations (@Validated on service/bean) ─
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getConstraintViolations().forEach(cv -> {
            // Path format: "methodName.paramName" — ambil bagian terakhir
            String path = cv.getPropertyPath().toString();
            String field = path.contains(".")
                    ? path.substring(path.lastIndexOf('.') + 1)
                    : path;
            fieldErrors.put(field, cv.getMessage());
        });

        log.warn("Constraint violation: {}", fieldErrors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        msg.get(ErrorCode.VALIDATION_ERROR.getMessageKey()),
                        fieldErrors));
    }

    // ── 5. Unreadable request body (malformed JSON) ────────────
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(
            HttpMessageNotReadableException ex) {

        log.warn("Unreadable request body: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(msg.get("error.request.unreadable")));
    }

    // ── 6. Wrong Content-Type (e.g. text/plain instead of application/json) ─
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex) {

        String detail = "Content-Type '" + ex.getContentType()
                + "' not supported. Supported: " + ex.getSupportedMediaTypes();
        log.warn("Unsupported media type: {}", detail);
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error(detail));
    }

    // ── 7. Wrong HTTP method (e.g. GET instead of POST) ────────
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {

        String detail = "Method '" + ex.getMethod()
                + "' not allowed. Supported: " + ex.getSupportedHttpMethods();
        log.warn("Method not allowed: {}", detail);
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(detail));
    }

    // ── 8. Type mismatch (e.g. "abc" passed as Integer / enum) ─
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        String expectedType = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "unknown";
        String detail = "Parameter '" + ex.getName()
                + "' expects " + expectedType
                + " but got '" + ex.getValue() + "'";

        log.warn("Type mismatch: {}", detail);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(detail));
    }

    // ── 9. Spring Security: bad credentials ───────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        // Tidak log — login gagal adalah kejadian normal, bukan error
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(msg.get(ErrorCode.INVALID_CREDENTIALS.getMessageKey())));
    }

    // ── 10. Spring Security: account disabled ──────────────────
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(DisabledException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(msg.get(ErrorCode.ACCOUNT_DISABLED.getMessageKey())));
    }

    // ── 11. Spring Security: access denied (403) ───────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(msg.get(ErrorCode.FORBIDDEN.getMessageKey())));
    }

    // ── 12. ResponseStatusException (dynamic HTTP status) ──────
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(
            ResponseStatusException ex) {

        log.warn("ResponseStatusException [{}]: {}", ex.getStatusCode(), ex.getReason());
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ApiResponse.error(
                        ex.getReason() != null ? ex.getReason() : ex.getMessage()));
    }

    // ── 13. Catch-all fallback (500) ────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(msg.get(ErrorCode.INTERNAL_SERVER_ERROR.getMessageKey())));
    }
}