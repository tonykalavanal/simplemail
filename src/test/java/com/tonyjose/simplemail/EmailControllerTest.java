package com.tonyjose.simplemail;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web-layer test: starts only the controller, with EmailService replaced by a mock. */
@WebMvcTest(EmailController.class)
class EmailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmailService emailService;

    private static final String VALID = """
            {"from":"me@example.com","to":"you@example.com","subject":"Hello","body":"Hi there"}
            """;

    @Test
    void validRequestReturns202() throws Exception {
        mockMvc.perform(post("/api/emails").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("sent"));

        verify(emailService).send(any(EmailRequest.class));
    }

    @Test
    void invalidRequestReturns400AndDoesNotSend() throws Exception {
        String invalid = """
                {"from":"not-an-email","to":"","subject":"","body":""}
                """;

        mockMvc.perform(post("/api/emails").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("invalid"))
                .andExpect(jsonPath("$.errors").isArray());

        verify(emailService, never()).send(any());
    }

    @Test
    void mailServerFailureReturns502() throws Exception {
        doThrow(new MailSendException("SMTP down")).when(emailService).send(any());

        mockMvc.perform(post("/api/emails").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("failed"));
    }
}
