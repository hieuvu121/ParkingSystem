package ParkingSystem.demo.entity;

import ParkingSystem.demo.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityTest {

    @Test
    void getAuthorities_returnsCorrectRole() {
        UserEntity user = UserEntity.builder()
                .fullName("Test")
                .email("test@example.com")
                .password("hashed")
                .role(Role.USERS)
                .build();

        assertThat(user.getAuthorities())
                .hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USERS");
    }

    @Test
    void getUsername_returnsEmail() {
        UserEntity user = UserEntity.builder().email("test@example.com").build();
        assertThat(user.getUsername()).isEqualTo("test@example.com");
    }

    @Test
    void isEnabled_defaultsFalse() {
        UserEntity user = UserEntity.builder().email("a@b.com").build();
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    void isEnabled_whenIsActiveTrue_returnsTrue() {
        UserEntity user = UserEntity.builder().email("a@b.com").isActive(true).build();
        assertThat(user.isEnabled()).isTrue();
    }
}
