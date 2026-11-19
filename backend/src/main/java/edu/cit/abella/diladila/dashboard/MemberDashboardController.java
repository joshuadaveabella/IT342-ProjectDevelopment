package edu.cit.abella.diladila.dashboard;

import edu.cit.abella.diladila.auth.AuthService;
import edu.cit.abella.diladila.user.AppUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Team Member dashboard. Leads may also call it (a lead is also a member, SRS 2.2). */
@RestController
@RequestMapping("/api/member")
@PreAuthorize("hasAnyRole('MEMBER','LEAD')")
public class MemberDashboardController {

    private final AuthService authService;

    public MemberDashboardController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(Authentication auth) {
        AppUser u = authService.getByEmail(auth.getName());
        return Map.of(
                "title", "Team Member Dashboard",
                "welcome", "Welcome, " + u.getFullName(),
                "role", u.getRole(),
                "capabilities", List.of(
                        "View projects you belong to",
                        "View and update your assigned tasks",
                        "Join task discussions and decision logs",
                        "Receive reminders"));
    }
}
