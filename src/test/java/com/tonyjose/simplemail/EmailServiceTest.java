package com.tonyjose.simplemail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/** Unit test: the mail sender is a Mockito mock, so no email is ever sent. */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void buildsTheMessageFromTheRequestAndSendsIt() {
        EmailRequest request = new EmailRequest("me@example.com", "you@example.com", "Hello", "Hi there");

        emailService.send(request);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();

        assertThat(sent.getFrom()).isEqualTo("me@example.com");
        assertThat(sent.getTo()).containsExactly("you@example.com");
        assertThat(sent.getSubject()).isEqualTo("Hello");
        assertThat(sent.getText()).isEqualTo("Hi there");
    }
}
