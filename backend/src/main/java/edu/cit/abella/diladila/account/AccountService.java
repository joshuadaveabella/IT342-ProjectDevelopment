package edu.cit.abella.diladila.account;

import edu.cit.abella.diladila.user.AppUser;
import edu.cit.abella.diladila.user.AppUserRepository;
import edu.cit.abella.diladila.user.PasswordResetToken;
import edu.cit.abella.diladila.user.PasswordResetTokenRepository;
import edu.cit.abella.diladila.web.FieldValidationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Operations on the signed-in user's own account. Every method takes the email of the authenticated
 * principal, never an id from the request, so one user cannot modify another user's account.
 */
@Service
public class AccountService {

    private final AppUserRepository users;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder encoder;

    public AccountService(AppUserRepository users, PasswordResetTokenRepository resetTokens, PasswordEncoder encoder) {
        this.users = users;
        this.resetTokens = resetTokens;
        this.encoder = encoder;
    }

    @Transactional
    public AppUser updateProfile(String email, ProfileRequest req) {
        AppUser user = users.findByEmailIgnoreCase(email).orElseThrow();
        user.updateProfile(req.fullName().trim(), blankToNull(req.phone()), blankToNull(req.bio()),
                blankToNull(req.avatarUrl()));
        return users.saveAndFlush(user);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest req) {
        AppUser user = users.findByEmailIgnoreCase(email).orElseThrow();

        if (!encoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new FieldValidationException("currentPassword", "Current password is incorrect");
        }
        if (!req.newPassword().equals(req.confirmPassword())) {
            throw new FieldValidationException("confirmPassword", "Passwords do not match");
        }
        if (encoder.matches(req.newPassword(), user.getPasswordHash())) {
            throw new FieldValidationException("newPassword", "New password must be different from the current password");
        }

        user.changePasswordHash(encoder.encode(req.newPassword()));
        users.save(user);

        // Any reset link requested before this change must not work any more
        Instant now = Instant.now();
        for (PasswordResetToken t : resetTokens.findByUserAndUsedAtIsNull(user)) {
            t.markUsed(now);
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
