package edu.cit.abella.diladila.web;

import edu.cit.abella.diladila.auth.DuplicateEmailException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400, "error", "Please correct the highlighted fields.", "fields", fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("status", 400, "error", "Malformed request body."));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> duplicate(DuplicateEmailException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", 409, "error", ex.getMessage(), "fields", Map.of("email", ex.getMessage())));
    }

    /** One generic message for unknown email and wrong password, so accounts cannot be enumerated. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> badLogin(AuthenticationException ex) {
        String msg = ex instanceof DisabledException
                ? "This account is disabled."
                : "Invalid email or password.";
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", 401, "error", msg));
    }
}
