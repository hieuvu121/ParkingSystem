package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.user.UpdateProfileRequest;
import ParkingSystem.demo.dto.user.UpdateRoleRequest;
import ParkingSystem.demo.dto.user.UserProfileResponse;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/api/users/me")
    public ResponseEntity<UserProfileResponse> getMe(@AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(userService.getProfile(user.getId()));
    }

    @PutMapping("/api/users/me")
    public ResponseEntity<UserProfileResponse> updateMe(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(user.getId(), request.getFullName()));
    }

    @GetMapping("/api/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileResponse>> listUsers() {
        return ResponseEntity.ok(userService.listAll());
    }

    @PatchMapping("/api/admin/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(userService.changeRole(id, request.getRole()));
    }

    @DeleteMapping("/api/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
