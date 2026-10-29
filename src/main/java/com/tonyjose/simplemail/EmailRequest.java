package com.tonyjose.simplemail;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The JSON body sent by the form. Validation rules run before the controller method,
 * so bad input never reaches the mail server.
 */
public record EmailRequest(
        @NotBlank(message = "Sender email is required")
        @Email(message = "Sender email is not valid")
        String from,

        @NotBlank(message = "Recipient email is required")
        @Email(message = "Recipient email is not valid")
        String to,

        @NotBlank(message = "Subject is required")
        @Size(max = 200, message = "Subject must be 200 characters or fewer")
        String subject,

        @NotBlank(message = "Message body is required")
        @Size(max = 10000, message = "Message body must be 10,000 characters or fewer")
        String body
) {}
