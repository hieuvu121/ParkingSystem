package ParkingSystem.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@parking.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:8080");
    }

    @Test
    void sendVerificationEmail_sendsToCorrectRecipient() {
        emailService.sendVerificationEmail("user@example.com", "abc-token-123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getTo()).contains("user@example.com");
    }

    @Test
    void sendVerificationEmail_bodyContainsToken() {
        emailService.sendVerificationEmail("user@example.com", "abc-token-123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getText()).contains("abc-token-123");
    }

    @Test
    void sendVerificationEmail_subjectIsNotBlank() {
        emailService.sendVerificationEmail("user@example.com", "abc-token-123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getSubject()).isNotBlank();
    }
}
