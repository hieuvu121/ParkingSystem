package ParkingSystem.demo.dto.spot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SpotRequest {
    @NotNull private Long row;
    @NotNull private Long col;
    @NotBlank private String type;
}
