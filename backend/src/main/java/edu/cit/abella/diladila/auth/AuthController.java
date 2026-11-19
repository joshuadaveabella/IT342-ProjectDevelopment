package edu.cit.abella.diladila.auth;

import edu.cit.abella.diladila.auth.dto.LoginRequest;
import edu.cit.abella.diladila.auth.dto.RegisterRequest;
import edu.cit.abella.diladila.auth.dto.UserResponse;
import edu.cit.abella.diladila.user.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authManager;
    private final SecurityContextRepository contextRepo;

    public AuthController(AuthService authService, AuthenticationManager authManager,
                          SecurityContextRepository contextRepo) {
        this.authService = authService;
        this.authManager = authManager;
        this.contextRepo = contextRepo;
    }

    /** Lets the SPA obtain the XSRF-TOKEN cookie before its first POST. */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        AppUser created = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(created));
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest req,
                              HttpServletRequest request, HttpServletResponse response) {
        // Throws BadCredentialsException (-> 401) for unknown email or wrong password
        Authentication auth = authManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(req.email().trim(), req.password()));

        // Session fixation protection: new session id after successful authentication
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        contextRepo.saveContext(context, request, response);

        return UserResponse.from(authService.getByEmail(auth.getName()));
    }

    /** Current signed-in user; 401 when there is no valid session. */
    @GetMapping("/me")
    public UserResponse me(Authentication auth) {
        return UserResponse.from(authService.getByEmail(auth.getName()));
    }

    // POST /api/auth/logout is handled by the logout filter configured in SecurityConfig.
}
