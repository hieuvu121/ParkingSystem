package ParkingSystem.demo.dto.zone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ZoneRequest {
    @NotNull
    private Long level;
    @NotBlank
    private String type;
}
