package ParkingSystem.demo.dto.user;

import ParkingSystem.demo.enums.Role;

public record UserProfileResponse(Long id, String fullName, String email, Role role) {}
