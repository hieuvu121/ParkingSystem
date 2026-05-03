package ParkingSystem.demo.dto.user;

import ParkingSystem.demo.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRoleRequest {
    @NotNull
    private Role role;
}
