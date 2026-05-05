package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.prediction.AvailabilityPredictionResponse;
import ParkingSystem.demo.repository.BookingRepository;
import ParkingSystem.demo.repository.ParkingSpotRepository;
import ParkingSystem.demo.repository.ParkingZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PredictionService {

    private final BookingRepository bookingRepository;
    private final ParkingSpotRepository spotRepository;
    private final ParkingZoneRepository zoneRepository;

    public AvailabilityPredictionResponse predict(Long zoneId, LocalDateTime targetTime) {
        long totalSpots = spotRepository.countByZone_idId(zoneId);
        if (totalSpots == 0) {
            return new AvailabilityPredictionResponse(zoneId, targetTime.toString(), 0.0);
        }
        // Java Mon=1..Sun=7 → PostgreSQL DOW Sun=0..Sat=6
        int javaDow = targetTime.getDayOfWeek().getValue();
        int pgDow = javaDow % 7;
        int hour = targetTime.getHour();
        long historicalBooked = bookingRepository.countHistoricalBookings(zoneId, pgDow, hour);
        double avgBooked = Math.min(historicalBooked, totalSpots);
        double probability = (totalSpots - avgBooked) / (double) totalSpots;
        return new AvailabilityPredictionResponse(zoneId, targetTime.toString(),
                Math.max(0.0, Math.min(1.0, probability)));
    }
}
