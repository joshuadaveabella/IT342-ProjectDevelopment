package edu.cit.abella.diladila.dashboard;

import edu.cit.abella.diladila.auth.AuthService;
import edu.cit.abella.diladila.auth.dto.UserResponse;
import edu.cit.abella.diladila.user.AppUser;
import edu.cit.abella.diladila.user.AppUserRepository;
import edu.cit.abella.diladila.user.Role;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Project Lead dashboard and lead-only operations. MEMBER accounts receive 403. */
@RestController
@RequestMapping("/api/lead")
@PreAuthorize("hasRole('LEAD')")
public class LeadDashboardController {

    private final AuthService authService;
    private final AppUserRepository users;

    public LeadDashboardController(AuthService authService, AppUserRepository users) {
        this.authService = authService;
        this.users = users;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(Authentication auth) {
        AppUser u = authService.getByEmail(auth.getName());
        return Map.of(
                "title", "Project Lead Dashboard",
                "welcome", "Welcome, " + u.getFullName(),
                "role", u.getRole(),
                "totalAccounts", users.count(),
                "leads", users.countByRole(Role.LEAD),
                "members", users.countByRole(Role.MEMBER),
                "capabilities", List.of(
                        "Create and manage projects",
                        "Invite and remove team members",
                        "Assign tasks and set deadlines",
                        "Record decisions and review progress"));
    }

    /** Lead-only operation: list registered accounts (used to pick team members). */
    @GetMapping("/users")
    public List<UserResponse> listUsers() {
        return users.findAllByOrderByCreatedAtDesc().stream().map(UserResponse::publicView).toList();
    }
}
