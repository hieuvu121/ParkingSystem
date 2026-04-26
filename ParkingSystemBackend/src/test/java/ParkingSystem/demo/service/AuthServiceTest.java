package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.AuthResponse;
import ParkingSystem.demo.dto.LoginRequest;
import ParkingSystem.demo.dto.RegisterRequest;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.enums.Role;
import ParkingSystem.demo.repository.UserRepository;
import ParkingSystem.demo.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_withNewEmail_savesUserWithHashedPasswordAndSendsEmail() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashedPw");

        Map<String, String> result = authService.register(
                new RegisterRequest("John", "john@example.com", "password123"));

        assertThat(result.get("message")).contains("Registration successful");
        verify(userRepository).save(argThat(user ->
                user.getEmail().equals("john@example.com") &&
                user.getPassword().equals("hashedPw") &&
                !user.isActive() &&
                user.getVerificationToken() != null));
        verify(emailService).sendVerificationEmail(eq("john@example.com"), anyString());
    }

    @Test
    void register_withExistingEmail_throwsConflict() {
        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(new UserEntity()));

        assertThatThrownBy(() ->
                authService.register(new RegisterRequest("John", "john@example.com", "password123")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex ->
                        assertThat(((ResponseStatusException) ex).getStatusCode())
                                .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void verify_withValidToken_activatesUserAndClearsToken() {
        UserEntity user = UserEntity.builder()
                .email("john@example.com")
                .verificationToken("valid-uuid")
                .role(Role.USERS)
                .fullName("John")
                .password("hashed")
                .build();
        when(userRepository.findByVerificationToken("valid-uuid")).thenReturn(Optional.of(user));

        Map<String, String> result = authService.verify("valid-uuid");

        assertThat(result.get("message")).contains("verified successfully");
        verify(userRepository).save(argThat(u -> u.isActive() && u.getVerificationToken() == null));
    }

    @Test
    void verify_withInvalidToken_throwsBadRequest() {
        when(userRepository.findByVerificationToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verify("bad-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex ->
                        assertThat(((ResponseStatusException) ex).getStatusCode())
                                .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void login_withValidCredentialsAndActiveUser_returnsJwt() {
        UserEntity user = UserEntity.builder()
                .email("john@example.com")
                .password("hashedPw")
                .isActive(true)
                .role(Role.USERS)
                .fullName("John")
                .build();
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPw")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt.token.here");

        AuthResponse result = authService.login(new LoginRequest("john@example.com", "password123"));

        assertThat(result.getToken()).isEqualTo("jwt.token.here");
    }

    @Test
    void login_withUnknownEmail_throwsUnauthorized() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("unknown@example.com", "pass")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex ->
                        assertThat(((ResponseStatusException) ex).getStatusCode())
                                .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void login_withWrongPassword_throwsUnauthorized() {
        UserEntity user = UserEntity.builder()
                .email("john@example.com")
                .password("hashedPw")
                .isActive(true)
                .build();
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashedPw")).thenReturn(false);

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("john@example.com", "wrongpass")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex ->
                        assertThat(((ResponseStatusException) ex).getStatusCode())
                                .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void login_withUnverifiedAccount_throwsForbidden() {
        UserEntity user = UserEntity.builder()
                .email("john@example.com")
                .password("hashedPw")
                .isActive(false)
                .build();
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPw")).thenReturn(true);

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("john@example.com", "password123")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex ->
                        assertThat(((ResponseStatusException) ex).getStatusCode())
                                .isEqualTo(HttpStatus.FORBIDDEN));
    }
}
