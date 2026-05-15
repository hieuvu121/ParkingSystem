package ParkingSystem.demo.dto.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequest {
    @NotNull private Long spotId;
    @NotNull @FutureOrPresent private LocalDateTime startTime;
    @NotNull @Future private LocalDateTime endTime;
    @NotNull @Pattern(regexp = "SUBSCRIPTION|PAY_PER_USE",
                     message = "paymentType must be SUBSCRIPTION or PAY_PER_USE")
    private String paymentType;
}
