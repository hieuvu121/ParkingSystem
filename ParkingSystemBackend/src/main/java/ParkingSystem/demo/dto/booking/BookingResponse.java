package ParkingSystem.demo.dto.booking;

import ParkingSystem.demo.enums.BookingStatus;

import java.time.LocalDateTime;

public record BookingResponse(Long id, Long spotId, Long userId,
                               LocalDateTime startTime, LocalDateTime endTime,
                               BookingStatus status) {}
