package edu.cit.abella.diladila;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    @Autowired MockMvc mvc;

    private static String reg(String name, String email, String pw, String role) {
        return "{\"fullName\":\"" + name + "\",\"email\":\"" + email + "\",\"password\":\"" + pw
                + "\",\"role\":" + (role == null ? "null" : "\"" + role + "\"") + "}";
    }

    private void register(String name, String email, String role) throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(reg(name, email, "Passw0rdOK", role))).andExpect(status().isCreated());
    }

    private MockHttpSession login(String email, String pw) throws Exception {
        var res = mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + pw + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) res.getRequest().getSession(false);
    }

    @Test
    void registrationValidatesAndRejectsDuplicates() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(reg("", "bad", "short", null))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists());
        register("Dup User", "dup@example.com", null);
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(reg("Dup Again", "DUP@example.com", "Passw0rdOK", null)))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordGivesGeneric401() throws Exception {
        register("Wrong Pw", "wp@example.com", null);
        mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"wp@example.com\",\"password\":\"nope12345\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password."));
    }

    @Test
    void protectedEndpointsRequireLogin() throws Exception {
        mvc.perform(get("/api/member/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void memberCannotReachLeadEndpointsButLeadCanReachBoth() throws Exception {
        register("Mem Ber", "mem@example.com", "MEMBER");
        register("Lee Dee", "lead@example.com", "LEAD");

        MockHttpSession m = login("mem@example.com", "Passw0rdOK");
        mvc.perform(get("/api/member/dashboard").session(m)).andExpect(status().isOk());
        mvc.perform(get("/api/lead/dashboard").session(m)).andExpect(status().isForbidden());
        mvc.perform(get("/api/lead/users").session(m)).andExpect(status().isForbidden());

        MockHttpSession l = login("lead@example.com", "Passw0rdOK");
        mvc.perform(get("/api/lead/dashboard").session(l)).andExpect(status().isOk());
        mvc.perform(get("/api/member/dashboard").session(l)).andExpect(status().isOk());
    }

    @Test
    void logoutEndsTheSession() throws Exception {
        register("Out User", "out@example.com", null);
        MockHttpSession s = login("out@example.com", "Passw0rdOK");
        mvc.perform(get("/api/auth/me").session(s)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").with(csrf()).session(s)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(s)).andExpect(status().isUnauthorized());
    }

    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@b.c\",\"password\":\"x\"}")).andExpect(status().isForbidden());
    }
}
