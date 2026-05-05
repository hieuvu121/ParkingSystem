package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.analytics.OccupancyResponse;
import ParkingSystem.demo.dto.analytics.PeakHourResponse;
import ParkingSystem.demo.dto.analytics.UtilizationResponse;
import ParkingSystem.demo.repository.BookingRepository;
import ParkingSystem.demo.repository.ParkingSpotRepository;
import ParkingSystem.demo.repository.ParkingZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final BookingRepository bookingRepository;
    private final ParkingSpotRepository spotRepository;
    private final ParkingZoneRepository zoneRepository;

    public List<OccupancyResponse> getOccupancy(LocalDateTime from, LocalDateTime to) {
        long totalSpots = spotRepository.count();
        if (totalSpots == 0) return List.of();
        List<Object[]> rows = bookingRepository.countByZoneInRange(from, to);
        return rows.stream().map(r -> {
            Long zoneId = ((Number) r[0]).longValue();
            long count = ((Number) r[1]).longValue();
            long zoneSpots = spotRepository.countByZone_idId(zoneId);
            double pct = zoneSpots > 0 ? (count * 100.0 / zoneSpots) : 0;
            return new OccupancyResponse(zoneId, from.toString(), to.toString(), pct);
        }).toList();
    }

    public List<PeakHourResponse> getPeakHours() {
        List<Object[]> rows = bookingRepository.countByHourOfDay();
        long maxCount = rows.stream().mapToLong(r -> ((Number) r[1]).longValue()).max().orElse(1L);
        return rows.stream().map(r -> {
            int hour = ((Number) r[0]).intValue();
            long count = ((Number) r[1]).longValue();
            return new PeakHourResponse(hour, count * 100.0 / maxCount);
        }).toList();
    }

    public List<UtilizationResponse> getUtilization() {
        return zoneRepository.findAll().stream().map(zone -> {
            long spots = spotRepository.countByZone_idId(zone.getId());
            long bookings = bookingRepository.countByZoneId(zone.getId());
            double pct = spots > 0 ? Math.min(bookings * 100.0 / spots, 100.0) : 0;
            return new UtilizationResponse(zone.getId(), spots, bookings, pct);
        }).toList();
    }
}
