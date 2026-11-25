package edu.cit.abella.diladila.auth.dto;

import edu.cit.abella.diladila.user.AppUser;
import edu.cit.abella.diladila.user.Role;

import java.time.Instant;

/** Public view of an account. Never includes the password hash. */
public record UserResponse(Long id, String fullName, String email, Role role,
                           String phone, String bio, String avatarUrl, Instant createdAt) {

    /** Full view, used for the signed-in user's own account. */
    public static UserResponse from(AppUser u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(),
                u.getPhone(), u.getBio(), u.getAvatarUrl(), u.getCreatedAt());
    }

    /** Limited view for lists shown to other users: no phone or bio. */
    public static UserResponse publicView(AppUser u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(),
                null, null, u.getAvatarUrl(), u.getCreatedAt());
    }
}
