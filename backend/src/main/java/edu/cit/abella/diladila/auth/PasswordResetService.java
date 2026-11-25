package edu.cit.abella.diladila.auth;

import edu.cit.abella.diladila.auth.dto.ResetPasswordRequest;
import edu.cit.abella.diladila.mail.ResetLinkSender;
import edu.cit.abella.diladila.user.AppUser;
import edu.cit.abella.diladila.user.AppUserRepository;
import edu.cit.abella.diladila.user.PasswordResetToken;
import edu.cit.abella.diladila.user.PasswordResetTokenRepository;
import edu.cit.abella.diladila.user.TokenHasher;
import edu.cit.abella.diladila.web.FieldValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Forgot / reset password.
 *  - A random 256-bit token is created; only its SHA-256 hash is stored.
 *  - The token expires (default 30 minutes) and works once.
 *  - Asking for a reset never reveals whether the email has an account.
 *  - At most one link per account per minute, and a new link cancels the older ones.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration COOLDOWN = Duration.ofSeconds(60);

    private final AppUserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final ResetLinkSender sender;
    private final String frontendUrl;
    private final long ttlMinutes;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(AppUserRepository users, PasswordResetTokenRepository tokens,
                                PasswordEncoder encoder, ResetLinkSender sender,
                                @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
                                @Value("${app.reset-token-minutes:30}") long ttlMinutes) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.sender = sender;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        this.ttlMinutes = ttlMinutes;
    }

    @Transactional
    public void requestReset(String email) {
        Optional<AppUser> found = users.findByEmailIgnoreCase(email.trim());
        if (found.isEmpty() || !found.get().isEnabled()) {
            return; // same visible result as a real account
        }
        AppUser user = found.get();
        Instant now = Instant.now();

        Optional<PasswordResetToken> last = tokens.findFirstByUserOrderByCreatedAtDesc(user);
        if (last.isPresent() && last.get().getCreatedAt().isAfter(now.minus(COOLDOWN))) {
            return; // too soon after the previous request
        }

        // A new request cancels older unused links
        for (PasswordResetToken old : tokens.findByUserAndUsedAtIsNull(user)) {
            old.markUsed(now);
        }

        byte[] raw = new byte[32];
        random.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        tokens.save(new PasswordResetToken(user, TokenHasher.sha256Hex(token), now.plus(Duration.ofMinutes(ttlMinutes))));

        try {
            sender.send(user.getEmail(), user.getFullName(), frontendUrl + "/reset-password?token=" + token, ttlMinutes);
        } catch (RuntimeException e) {
            log.error("Reset link delivery failed: {}", e.getMessage());
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        if (!req.newPassword().equals(req.confirmPassword())) {
            throw new FieldValidationException("confirmPassword", "Passwords do not match");
        }
        PasswordResetToken token = tokens.findByTokenHash(TokenHasher.sha256Hex(req.token().trim()))
                .orElseThrow(InvalidResetTokenException::new);
        Instant now = Instant.now();
        AppUser user = token.getUser();
        if (!token.isUsable(now) || !user.isEnabled()) {
            throw new InvalidResetTokenException();
        }

        user.changePasswordHash(encoder.encode(req.newPassword()));
        users.save(user);

        token.markUsed(now);
        for (PasswordResetToken other : tokens.findByUserAndUsedAtIsNull(user)) {
            other.markUsed(now);
        }
    }
}
