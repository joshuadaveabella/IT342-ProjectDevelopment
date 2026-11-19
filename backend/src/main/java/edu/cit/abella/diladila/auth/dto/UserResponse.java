package edu.cit.abella.diladila.auth.dto;

import edu.cit.abella.diladila.user.AppUser;
import edu.cit.abella.diladila.user.Role;

import java.time.Instant;

/** Public view of an account. Never includes the password hash. */
public record UserResponse(Long id, String fullName, String email, Role role, Instant createdAt) {
    public static UserResponse from(AppUser u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.getCreatedAt());
    }
}
