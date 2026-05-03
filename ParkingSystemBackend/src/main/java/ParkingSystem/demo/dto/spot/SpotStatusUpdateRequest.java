package ParkingSystem.demo.dto.spot;

import ParkingSystem.demo.enums.SpotStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SpotStatusUpdateRequest {
    @NotNull private Long spotId;
    @NotNull private SpotStatus status;
}
