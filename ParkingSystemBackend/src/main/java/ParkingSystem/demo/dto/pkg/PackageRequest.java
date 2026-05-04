package ParkingSystem.demo.dto.pkg;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PackageRequest {
    @NotBlank private String name;
    @NotBlank private String description;
    @NotNull @Positive private Long durations;
    @NotNull @Positive private Long price;
}
