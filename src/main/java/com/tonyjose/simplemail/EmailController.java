package com.tonyjose.simplemail;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** REST API used by the form: POST /api/emails */
@RestController
@RequestMapping("/api/emails")
public class EmailController {

    private static final Logger log = LoggerFactory.getLogger(EmailController.class);

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    /** 202 Accepted when the mail server accepts the email. */
    @PostMapping
    public ResponseEntity<Map<String, String>> send(@Valid @RequestBody EmailRequest request) {
        emailService.send(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("status", "sent"));
    }

    /** 400 Bad Request with a list of what's wrong, when validation fails. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getDefaultMessage())
                .sorted()
                .toList();
        return ResponseEntity.badRequest().body(Map.of("status", "invalid", "errors", errors));
    }

    /** 502 Bad Gateway when the SMTP server is down or rejects the email. */
    @ExceptionHandler(MailException.class)
    public ResponseEntity<Map<String, String>> handleMailFailure(MailException ex) {
        log.error("Mail server failed to send email", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("status", "failed", "error", "The mail server could not send the email"));
    }
}
