package edu.cit.abella.diladila.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends the reset link by email when SMTP is configured (spring.mail.host). When it is not configured
 * (local development) the link is printed in the backend console instead, so the flow can be demonstrated.
 */
@Component
public class MailResetLinkSender implements ResetLinkSender {

    private static final Logger log = LoggerFactory.getLogger(MailResetLinkSender.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    public MailResetLinkSender(ObjectProvider<JavaMailSender> mailSender,
                               @Value("${app.mail.from:no-reply@dila-dila.local}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(String toEmail, String fullName, String link, long validMinutes) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("SMTP is not configured. DEV MODE password reset link for {}: {}", toEmail, link);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(toEmail);
            msg.setSubject("Reset your Dila-Dila password");
            msg.setText("Hi " + fullName + ",\n\n"
                    + "We received a request to reset your Dila-Dila password. Open this link to choose a new one:\n\n"
                    + link + "\n\n"
                    + "The link works once and expires in " + validMinutes + " minutes.\n"
                    + "If you did not ask for this, you can ignore this email; your password stays the same.\n");
            sender.send(msg);
        } catch (Exception e) {
            // Never reveal delivery problems to the requester (that would leak whether the account exists)
            log.error("Could not send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }
}
