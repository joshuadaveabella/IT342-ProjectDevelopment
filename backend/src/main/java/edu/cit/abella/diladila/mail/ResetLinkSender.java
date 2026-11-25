package edu.cit.abella.diladila.mail;

/** Delivers a password-reset link to a user. */
public interface ResetLinkSender {
    void send(String toEmail, String fullName, String link, long validMinutes);
}
