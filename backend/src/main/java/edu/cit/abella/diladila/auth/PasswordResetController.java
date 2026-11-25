package edu.cit.abella.diladila.auth;

import edu.cit.abella.diladila.auth.dto.ForgotPasswordRequest;
import edu.cit.abella.diladila.auth.dto.ResetPasswordRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final PasswordResetService service;

    public PasswordResetController(PasswordResetService service) {
        this.service = service;
    }

    /** Always answers 202 with the same message, whether or not the email has an account. */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgot(@Valid @RequestBody ForgotPasswordRequest req) {
        service.requestReset(req.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "message", "If an account exists for that email, a password reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetPasswordRequest req) {
        service.resetPassword(req);
        return ResponseEntity.noContent().build();
    }
}
