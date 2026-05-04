package es.urjc.code.backend.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler for REST API endpoints (/api/v1/**).
 * Ensures all errors are returned as JSON, never HTML.
 */
@RestControllerAdvice(basePackages = "es.urjc.code.backend.rest")
public class GlobalApiExceptionHandler {

    // ── 400 Bad Request ─────────────────────────────────────
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(Exception ex, HttpServletRequest request) {
        return errorBody(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request.getRequestURI());
    }

    // ── 401 Unauthorized ────────────────────────────────────
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> handleUnauthorized(Exception ex, HttpServletRequest request) {
        return errorBody(HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication is required to access this resource.", request.getRequestURI());
    }

    // ── 403 Forbidden ───────────────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> handleForbidden(AccessDeniedException ex, HttpServletRequest request) {
        return errorBody(HttpStatus.FORBIDDEN, "Forbidden", "You do not have permission to perform this action.", request.getRequestURI());
    }

    // ── 404 Not Found ───────────────────────────────────────
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        Map<String, Object> body = errorBody(
                HttpStatus.valueOf(ex.getStatusCode().value()),
                ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    // ── 500 Internal Server Error ────────────────────────────
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleGeneric(Exception ex, HttpServletRequest request) {
        return errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.", request.getRequestURI());
    }

    // ── Helper ───────────────────────────────────────────────
    private Map<String, Object> errorBody(HttpStatus status, String error, String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", path);
        return body;
    }
}
