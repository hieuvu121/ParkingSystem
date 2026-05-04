package ParkingSystem.demo.dto.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequest {
    @NotNull private Long spotId;
    @NotNull private LocalDateTime startTime;
    @NotNull @Future private LocalDateTime endTime;
}
