package edu.cit.abella.diladila.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Editable profile fields only. Email and role are not part of this request, so a client cannot change
 * them even if it sends them (unknown JSON properties are ignored).
 */
public record ProfileRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        @Pattern(regexp = "^$|^\\+?[0-9 ()\\-]{7,20}$", message = "Enter a valid phone number")
        String phone,

        @Size(max = 255, message = "Bio must be at most 255 characters")
        String bio,

        @Size(max = 255, message = "Photo link must be at most 255 characters")
        @Pattern(regexp = "^$|^https://\\S+$", message = "Photo link must start with https://")
        String avatarUrl) {
}
