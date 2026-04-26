package ParkingSystem.demo.security;

import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.enums.Role;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        String testSecret = Base64.getEncoder().encodeToString(
                Jwts.SIG.HS256.key().build().getEncoded());
        jwtService = new JwtService(testSecret, 86400000L);

        testUser = UserEntity.builder()
                .email("test@example.com")
                .password("hashed")
                .role(Role.USERS)
                .fullName("Test User")
                .isActive(true)
                .build();
    }

    @Test
    void generateToken_returnsNonBlankToken() {
        assertThat(jwtService.generateToken(testUser)).isNotBlank();
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String token = jwtService.generateToken(testUser);
        assertThat(jwtService.extractEmail(token)).isEqualTo("test@example.com");
    }

    @Test
    void isTokenValid_withValidToken_returnsTrue() {
        String token = jwtService.generateToken(testUser);
        assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
    }

    @Test
    void isTokenValid_withExpiredToken_returnsFalse() {
        String expiredSecret = Base64.getEncoder().encodeToString(
                Jwts.SIG.HS256.key().build().getEncoded());
        JwtService expiredService = new JwtService(expiredSecret, -1000L);
        String token = expiredService.generateToken(testUser);
        assertThat(expiredService.isTokenValid(token, testUser)).isFalse();
    }

    @Test
    void isTokenValid_withWrongUser_returnsFalse() {
        String token = jwtService.generateToken(testUser);
        UserEntity otherUser = UserEntity.builder()
                .email("other@example.com")
                .password("hashed")
                .role(Role.USERS)
                .fullName("Other")
                .isActive(true)
                .build();
        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }
}
