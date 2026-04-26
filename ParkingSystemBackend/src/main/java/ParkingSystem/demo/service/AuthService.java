package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.AuthResponse;
import ParkingSystem.demo.dto.LoginRequest;
import ParkingSystem.demo.dto.RegisterRequest;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.enums.Role;
import ParkingSystem.demo.repository.UserRepository;
import ParkingSystem.demo.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    public Map<String, String> register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        String token = UUID.randomUUID().toString();
        UserEntity user = UserEntity.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USERS)
                .isActive(false)
                .verificationToken(token)
                .build();
        userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), token);
        return Map.of("message", "Registration successful. Please check your email to verify your account.");
    }

    public Map<String, String> verify(String token) {
        UserEntity user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid or already used verification token"));
        user.setActive(true);
        user.setVerificationToken(null);
        userRepository.save(user);
        return Map.of("message", "Account verified successfully. You can now log in.");
    }

    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account not yet verified. Please check your email.");
        }
        return new AuthResponse(jwtService.generateToken(user));
    }
}
