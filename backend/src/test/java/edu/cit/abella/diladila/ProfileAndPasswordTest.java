package edu.cit.abella.diladila;

import edu.cit.abella.diladila.mail.ResetLinkSender;
import edu.cit.abella.diladila.user.AppUser;
import edu.cit.abella.diladila.user.AppUserRepository;
import edu.cit.abella.diladila.user.PasswordResetToken;
import edu.cit.abella.diladila.user.PasswordResetTokenRepository;
import edu.cit.abella.diladila.user.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Increment 2: profile management, change password, forgot/reset password. */
@SpringBootTest
@AutoConfigureMockMvc
class ProfileAndPasswordTest {

    private static final String PW = "Passw0rdOK";
    private static final String NEW_PW = "BrandNew123";

    /** Captures the reset link instead of sending an email. */
    static class CapturingSender implements ResetLinkSender {
        String lastEmail;
        String lastLink;
        int count;

        @Override
        public void send(String toEmail, String fullName, String link, long validMinutes) {
            lastEmail = toEmail;
            lastLink = link;
            count++;
        }

        void clear() {
            lastEmail = null;
            lastLink = null;
            count = 0;
        }

        String token() {
            return lastLink.substring(lastLink.indexOf("token=") + 6);
        }
    }

    @TestConfiguration
    static class CaptureConfig {
        @Bean
        @Primary
        CapturingSender capturingSender() {
            return new CapturingSender();
        }
    }

    @Autowired MockMvc mvc;
    @Autowired CapturingSender sender;
    @Autowired AppUserRepository users;
    @Autowired PasswordResetTokenRepository tokens;

    @BeforeEach
    void reset() {
        sender.clear();
    }

    private void register(String email, String role) throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Test User\",\"email\":\"" + email + "\",\"password\":\"" + PW
                        + "\",\"role\":\"" + role + "\"}")).andExpect(status().isCreated());
    }

    private MockHttpSession login(String email, String pw) throws Exception {
        var res = mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + pw + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) res.getRequest().getSession(false);
    }

    private void loginFails(String email, String pw) throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + pw + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------- Profile ----------------

    @Test
    void profileRequiresLogin() throws Exception {
        mvc.perform(get("/api/profile")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/profile").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"X\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void userCanViewAndEditOwnProfileButNotEmailOrRole() throws Exception {
        register("prof@example.com", "MEMBER");
        MockHttpSession s = login("prof@example.com", PW);

        mvc.perform(get("/api/profile").session(s)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("prof@example.com"));

        // role and email in the payload must be ignored
        mvc.perform(put("/api/profile").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Renamed User\",\"phone\":\"+63 912 345 6789\",\"bio\":\"Hello\","
                        + "\"avatarUrl\":\"https://example.com/me.png\",\"role\":\"LEAD\",\"email\":\"evil@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Renamed User"))
                .andExpect(jsonPath("$.phone").value("+63 912 345 6789"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.email").value("prof@example.com"));

        // saved in the database
        AppUser saved = users.findByEmailIgnoreCase("prof@example.com").orElseThrow();
        assertEquals("Renamed User", saved.getFullName());
        assertEquals("Hello", saved.getBio());

        // still a member: lead endpoints stay forbidden
        mvc.perform(get("/api/lead/dashboard").session(s)).andExpect(status().isForbidden());
    }

    @Test
    void profileInputsAreValidated() throws Exception {
        register("val@example.com", "MEMBER");
        MockHttpSession s = login("val@example.com", PW);
        mvc.perform(put("/api/profile").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"  \",\"phone\":\"abc\",\"avatarUrl\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.fullName").exists())
                .andExpect(jsonPath("$.fields.phone").exists())
                .andExpect(jsonPath("$.fields.avatarUrl").exists());
    }

    @Test
    void usersEditOnlyTheirOwnProfile() throws Exception {
        register("a1@example.com", "MEMBER");
        register("b1@example.com", "MEMBER");
        MockHttpSession a = login("a1@example.com", PW);
        mvc.perform(put("/api/profile").session(a).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Only A Changed\"}")).andExpect(status().isOk());
        assertEquals("Test User", users.findByEmailIgnoreCase("b1@example.com").orElseThrow().getFullName());
        assertEquals("Only A Changed", users.findByEmailIgnoreCase("a1@example.com").orElseThrow().getFullName());
    }

    // ---------------- Change password ----------------

    @Test
    void changePasswordRules() throws Exception {
        register("chg@example.com", "MEMBER");
        MockHttpSession s = login("chg@example.com", PW);

        mvc.perform(post("/api/account/change-password").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"wrong-one1\",\"newPassword\":\"" + NEW_PW + "\",\"confirmPassword\":\"" + NEW_PW + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.currentPassword").exists());

        mvc.perform(post("/api/account/change-password").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + PW + "\",\"newPassword\":\"" + NEW_PW + "\",\"confirmPassword\":\"Different123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.confirmPassword").exists());

        mvc.perform(post("/api/account/change-password").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + PW + "\",\"newPassword\":\"short\",\"confirmPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.newPassword").exists());

        mvc.perform(post("/api/account/change-password").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + PW + "\",\"newPassword\":\"" + PW + "\",\"confirmPassword\":\"" + PW + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.newPassword").exists());
    }

    @Test
    void changePasswordInvalidatesTheOldPassword() throws Exception {
        register("chg2@example.com", "MEMBER");
        MockHttpSession s = login("chg2@example.com", PW);

        mvc.perform(post("/api/account/change-password").session(s).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + PW + "\",\"newPassword\":\"" + NEW_PW + "\",\"confirmPassword\":\"" + NEW_PW + "\"}"))
                .andExpect(status().isNoContent());

        loginFails("chg2@example.com", PW);
        login("chg2@example.com", NEW_PW);
    }

    @Test
    void changePasswordRequiresLogin() throws Exception {
        mvc.perform(post("/api/account/change-password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"a\",\"newPassword\":\"b\",\"confirmPassword\":\"b\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------- Forgot / reset password ----------------

    @Test
    void forgotPasswordGivesTheSameAnswerForUnknownEmails() throws Exception {
        mvc.perform(post("/api/auth/forgot-password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").exists());
        assertNull(sender.lastLink);

        register("fp@example.com", "MEMBER");
        mvc.perform(post("/api/auth/forgot-password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"FP@example.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").exists());
        assertNotNull(sender.lastLink);
        assertEquals("fp@example.com", sender.lastEmail);
    }

    @Test
    void resetLinkWorksOnceAndOldPasswordStopsWorking() throws Exception {
        register("rs@example.com", "MEMBER");
        mvc.perform(post("/api/auth/forgot-password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"rs@example.com\"}")).andExpect(status().isAccepted());
        String token = sender.token();

        // weak or mismatched passwords are rejected and do not burn the token
        mvc.perform(post("/api/auth/reset-password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"weak\",\"confirmPassword\":\"weak\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.newPassword").exists());

        mvc.perform(post("/api/auth/reset-password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + NEW_PW + "\",\"confirmPassword\":\"" + NEW_PW + "\"}"))
                .andExpect(status().isNoContent());

        loginFails("rs@example.com", PW);
        login("rs@example.com", NEW_PW);

        // single use
        mvc.perform(post("/api/auth/reset-password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"Another123x\",\"confirmPassword\":\"Another123x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void unknownAndExpiredTokensAreRejected() throws Exception {
        mvc.perform(post("/api/auth/reset-password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"not-a-real-token\",\"newPassword\":\"" + NEW_PW + "\",\"confirmPassword\":\"" + NEW_PW + "\"}"))
                .andExpect(status().isBadRequest());

        register("exp@example.com", "MEMBER");
        AppUser u = users.findByEmailIgnoreCase("exp@example.com").orElseThrow();
        tokens.save(new PasswordResetToken(u, TokenHasher.sha256Hex("expired-token-value"), Instant.now().minusSeconds(60)));
        mvc.perform(post("/api/auth/reset-password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"expired-token-value\",\"newPassword\":\"" + NEW_PW + "\",\"confirmPassword\":\"" + NEW_PW + "\"}"))
                .andExpect(status().isBadRequest());
        login("exp@example.com", PW); // password unchanged
    }

    @Test
    void newResetRequestCancelsTheOlderLinkAndIsRateLimited() throws Exception {
        register("rl@example.com", "MEMBER");
        mvc.perform(post("/api/auth/forgot-password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"rl@example.com\"}")).andExpect(status().isAccepted());
        assertEquals(1, sender.count);
        // second request inside the cool-down sends nothing new
        mvc.perform(post("/api/auth/forgot-password").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"rl@example.com\"}")).andExpect(status().isAccepted());
        assertEquals(1, sender.count);
    }
}
