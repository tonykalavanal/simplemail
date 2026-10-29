package com.tonyjose.simplemail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Builds the email and hands it to JavaMailSender, which talks SMTP to the mail server. */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    // Constructor injection: Spring passes in the JavaMailSender it configured from application.yml
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(EmailRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(request.from());
        message.setTo(request.to());
        message.setSubject(request.subject());
        message.setText(request.body());

        mailSender.send(message);
        // Log the fact of sending, never the body (it may contain personal data)
        log.info("Email sent to {} with subject '{}'", request.to(), request.subject());
    }
}
