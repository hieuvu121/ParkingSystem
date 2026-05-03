package ParkingSystem.demo.service;

import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.enums.Role;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private UserService userService;

    private UserEntity user(Long id, String name, Role role) {
        return UserEntity.builder().id(id).fullName(name).email("u@x.com").role(role)
                .password("pw").isActive(true).build();
    }

    @Test
    void getProfile_existingUser_returnsProfile() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "Alice", Role.USERS)));
        var result = userService.getProfile(1L);
        assertThat(result.fullName()).isEqualTo("Alice");
    }

    @Test
    void getProfile_unknownUser_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_existingUser_updatesFullName() {
        var u = user(1L, "Alice", Role.USERS);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = userService.updateProfile(1L, "Bob");
        assertThat(result.fullName()).isEqualTo("Bob");
    }

    @Test
    void changeRole_existingUser_updatesRole() {
        var u = user(1L, "Alice", Role.USERS);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = userService.changeRole(1L, Role.ADMIN);
        assertThat(result.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void deleteUser_existingUser_callsDelete() {
        when(userRepository.existsById(1L)).thenReturn(true);
        userService.deleteUser(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_unknownUser_throwsNotFound() {
        when(userRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listAll_returnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(
                user(1L, "Alice", Role.USERS),
                user(2L, "Bob", Role.ADMIN)
        ));
        var result = userService.listAll();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).fullName()).isEqualTo("Alice");
    }
}
