package ParkingSystem.demo.dto.user;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdatePlateRequest {
    @Pattern(regexp = "^[A-Z0-9\\-]{2,15}$", message = "Invalid plate format",
             flags = Pattern.Flag.CASE_INSENSITIVE)
    private String plateNumber; // null is permitted — clears the plate
}
