package ParkingSystem.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    public void sendVerificationEmail(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Verify your Parking System account");
        message.setText(
                "Thank you for registering!\n\n" +
                "Click the link below to verify your account:\n\n" +
                baseUrl + "/api/auth/verify?token=" + token + "\n\n" +
                "This link is valid for 24 hours."
        );
        mailSender.send(message);
    }
}
