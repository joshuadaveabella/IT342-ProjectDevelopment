package edu.cit.abella.diladila.account;

import edu.cit.abella.diladila.auth.dto.UserResponse;
import edu.cit.abella.diladila.user.AppUser;
import edu.cit.abella.diladila.user.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Profile and password endpoints for the signed-in user. There is no user id in any URL: the account
 * is always the authenticated principal, which prevents editing other users' data.
 */
@RestController
@RequestMapping("/api")
public class ProfileController {

    private final AccountService accounts;
    private final AppUserRepository users;

    public ProfileController(AccountService accounts, AppUserRepository users) {
        this.accounts = accounts;
        this.users = users;
    }

    @GetMapping("/profile")
    public UserResponse getProfile(Authentication auth) {
        AppUser user = users.findByEmailIgnoreCase(auth.getName()).orElseThrow();
        return UserResponse.from(user);
    }

    @PutMapping("/profile")
    public UserResponse updateProfile(@Valid @RequestBody ProfileRequest req, Authentication auth) {
        return UserResponse.from(accounts.updateProfile(auth.getName(), req));
    }

    @PostMapping("/account/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req,
                                               Authentication auth, HttpServletRequest request) {
        accounts.changePassword(auth.getName(), req);
        // New session id after a credential change
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        return ResponseEntity.noContent().build();
    }
}
