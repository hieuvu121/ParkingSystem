package ParkingSystem.demo.scheduler;

import ParkingSystem.demo.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class BookingExpirationJob {

    private final BookingService bookingService;

    @Scheduled(fixedRate = 300000)
    public void expireOverdueBookings() {
        bookingService.expireOverdue();
    }
}
