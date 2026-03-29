package com.mohammadnuridin.todolistapp.core.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Aspect terpadu untuk logging dan performance monitoring.
 *
 * Layer yang dicakup:
 * Controller → trace entry/exit + mask sensitive args
 * Service → trace entry/exit + slow warning
 * Repository → slow query warning
 *
 * Satu @Around per layer — tidak ada duplikasi advice.
 * Semua @Before, @AfterReturning, @AfterThrowing dihapus karena
 * 
 * @Around sudah mencakup semua siklus (entry, exit, exception).
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Value("${app.logging.slow-query-threshold-ms:500}")
    private long slowQueryThresholdMs;

    @Value("${app.logging.slow-service-threshold-ms:2000}")
    private long slowServiceThresholdMs;

    // ═══════════════════════════════════════════════════════════
    // POINTCUTS
    // ═══════════════════════════════════════════════════════════

    @Pointcut("execution(* com.mohammadnuridin.todolistapp..api.*Controller.*(..))")
    public void controllerMethods() {
    }

    @Pointcut("execution(* com.mohammadnuridin.todolistapp..service.*ServiceImpl.*(..))")
    public void serviceMethods() {
    }

    @Pointcut("execution(* com.mohammadnuridin.todolistapp..repository.*Repository.*(..))")
    public void repositoryMethods() {
    }

    // ═══════════════════════════════════════════════════════════
    // CONTROLLER — trace entry/exit, mask sensitive args
    // ═══════════════════════════════════════════════════════════

    @Around("controllerMethods()")
    public Object traceController(ProceedingJoinPoint pjp) throws Throwable {
        String label = pjp.getTarget().getClass().getSimpleName()
                + "." + pjp.getSignature().getName() + "()";

        if (log.isDebugEnabled()) {
            log.debug("[CTRL] → {} args=[{}]", label, buildMaskedArgs(pjp.getArgs()));
        }

        try {
            Object result = pjp.proceed();
            log.debug("[CTRL] ← {} OK", label);
            return result;

        } catch (Throwable ex) {
            // Tidak log stack trace di sini — GlobalExceptionHandler yang handle
            log.warn("[CTRL_ERR] {} — {}: {}", label,
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // SERVICE — trace entry/exit + slow warning + audit
    // ═══════════════════════════════════════════════════════════

    @Around("serviceMethods()")
    public Object traceService(ProceedingJoinPoint pjp) throws Throwable {
        String className = pjp.getTarget().getClass().getSimpleName();
        String methodName = pjp.getSignature().getName();
        String label = className + "." + methodName + "()";
        long start = System.currentTimeMillis();

        log.debug("[SVC] → {}", label);

        try {
            Object result = pjp.proceed();
            long duration = System.currentTimeMillis() - start;

            if (duration >= slowServiceThresholdMs) {
                log.warn("SLOW_SERVICE {} took {}ms (threshold={}ms)",
                        label, duration, slowServiceThresholdMs);
            } else {
                log.debug("[SVC] ← {} took {}ms", label, duration);
            }

            // Audit log untuk operasi sensitif
            auditIfSensitive(methodName, className);

            return result;

        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - start;
            log.warn("[SVC_ERR] {} after {}ms — {}: {}",
                    label, duration,
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // REPOSITORY — slow query warning
    // ═══════════════════════════════════════════════════════════

    @Around("repositoryMethods()")
    public Object monitorRepository(ProceedingJoinPoint pjp) throws Throwable {
        String label = pjp.getTarget().getClass().getSimpleName()
                + "." + pjp.getSignature().getName() + "()";
        long start = System.currentTimeMillis();

        try {
            Object result = pjp.proceed();
            long duration = System.currentTimeMillis() - start;

            if (duration >= slowQueryThresholdMs) {
                log.warn("SLOW_QUERY {} took {}ms (threshold={}ms)",
                        label, duration, slowQueryThresholdMs);
            } else {
                log.debug("QUERY {} took {}ms", label, duration);
            }

            return result;

        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - start;
            log.error("QUERY_ERR {} after {}ms — {}: {}",
                    label, duration,
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════

    /**
     * Audit log untuk operasi sensitif — login, logout, register,
     * changePassword, deleteAccount.
     * Pakai WARN supaya muncul di semua environment termasuk prod.
     */
    private void auditIfSensitive(String methodName, String className) {
        boolean isSensitive = switch (methodName) {
            case "login", "logout", "register",
                    "refreshToken", "changePassword",
                    "deleteAccount" ->
                true;
            default -> false;
        };

        if (isSensitive) {
            log.warn("[AUDIT] {}.{}()", className, methodName);
        }
    }

    /**
     * Bangun string args dengan mask untuk field sensitif.
     * Aman untuk di-log — password dan token tidak akan muncul.
     */
    private String buildMaskedArgs(Object[] args) {
        if (args == null || args.length == 0)
            return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0)
                sb.append(", ");
            String str = args[i] == null ? "null" : args[i].toString();
            sb.append(maskSensitiveData(str));
        }
        return sb.toString();
    }

    /**
     * Mask field sensitif: password, token, refreshToken, accessToken.
     * Mendukung format JSON ("key":"value") dan Record toString (key=value).
     */
    private String maskSensitiveData(String data) {
        if (data == null)
            return "null";

        return data
                // JSON format: "password":"value"
                .replaceAll("(?i)(\"password\"\\s*:\\s*\")[^\"]*\"", "$1***\"")
                .replaceAll("(?i)(\"currentPassword\"\\s*:\\s*\")[^\"]*\"", "$1***\"")
                .replaceAll("(?i)(\"newPassword\"\\s*:\\s*\")[^\"]*\"", "$1***\"")
                .replaceAll("(?i)(\"refreshToken\"\\s*:\\s*\")[^\"]*\"", "$1***\"")
                .replaceAll("(?i)(\"accessToken\"\\s*:\\s*\")[^\"]*\"", "$1***\"")
                .replaceAll("(?i)(\"token\"\\s*:\\s*\")[^\"]*\"", "$1***\"")
                // Record/toString format: password=value
                .replaceAll("(?i)(\\bpassword=)[^,)\\s]*", "$1***")
                .replaceAll("(?i)(\\brefreshToken=)[^,)\\s]*", "$1***")
                .replaceAll("(?i)(\\baccessToken=)[^,)\\s]*", "$1***");
    }
}