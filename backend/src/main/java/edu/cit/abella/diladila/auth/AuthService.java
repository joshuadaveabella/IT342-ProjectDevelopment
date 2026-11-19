package edu.cit.abella.diladila.auth;

import edu.cit.abella.diladila.auth.dto.RegisterRequest;
import edu.cit.abella.diladila.user.AppUser;
import edu.cit.abella.diladila.user.AppUserRepository;
import edu.cit.abella.diladila.user.Role;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public AuthService(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional
    public AppUser register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }
        Role role = req.role() == null ? Role.MEMBER : req.role();
        AppUser user = new AppUser(req.fullName().trim(), email, encoder.encode(req.password()), role);
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException race) {
            // two simultaneous sign-ups: the unique constraint is the final guard
            throw new DuplicateEmailException();
        }
    }

    public AppUser getByEmail(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow();
    }
}
