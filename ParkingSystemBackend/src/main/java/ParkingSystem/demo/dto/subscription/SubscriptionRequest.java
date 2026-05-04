package ParkingSystem.demo.dto.subscription;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscriptionRequest {
    @NotNull private Long packageId;
}
