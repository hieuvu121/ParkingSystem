package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.user.UserProfileResponse;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.enums.Role;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserProfileResponse getProfile(Long userId) {
        return toResponse(findOrThrow(userId));
    }

    public UserProfileResponse updateProfile(Long userId, String fullName) {
        UserEntity user = findOrThrow(userId);
        user.setFullName(fullName);
        return toResponse(userRepository.save(user));
    }

    public UserProfileResponse changeRole(Long userId, Role role) {
        UserEntity user = findOrThrow(userId);
        user.setRole(role);
        return toResponse(userRepository.save(user));
    }

    public List<UserProfileResponse> listAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
    }

    private UserEntity findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private UserProfileResponse toResponse(UserEntity u) {
        return new UserProfileResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole());
    }
}
