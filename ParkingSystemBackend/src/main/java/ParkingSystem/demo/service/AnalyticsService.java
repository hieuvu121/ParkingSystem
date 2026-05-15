package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.analytics.OccupancyResponse;
import ParkingSystem.demo.dto.analytics.PeakHourResponse;
import ParkingSystem.demo.dto.analytics.UtilizationResponse;
import ParkingSystem.demo.repository.BookingRepository;
import ParkingSystem.demo.repository.ParkingSpotRepository;
import ParkingSystem.demo.repository.ParkingZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final BookingRepository bookingRepository;
    private final ParkingSpotRepository spotRepository;
    private final ParkingZoneRepository zoneRepository;

    public List<OccupancyResponse> getOccupancy(LocalDateTime from, LocalDateTime to) {
        if (spotRepository.count() == 0) return List.of();
        double periodHours = Duration.between(from, to).toMinutes() / 60.0;
        if (periodHours <= 0) return List.of();
        List<Object[]> rows = bookingRepository.bookedHoursByZoneInRange(from, to);
        return rows.stream().map(r -> {
            Long zoneId = ((Number) r[0]).longValue();
            double bookedHours = ((Number) r[1]).doubleValue();
            long zoneSpots = spotRepository.countByZone_idId(zoneId);
            double pct = zoneSpots > 0 ? Math.min(bookedHours * 100.0 / (zoneSpots * periodHours), 100.0) : 0;
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

    public List<UtilizationResponse> getUtilization(LocalDateTime from, LocalDateTime to) {
        double periodHours = Duration.between(from, to).toMinutes() / 60.0;
        if (periodHours <= 0) return List.of();
        List<Object[]> rows = bookingRepository.bookedHoursByZoneInRange(from, to);
        Map<Long, Double> hookedByZone = rows.stream()
                .collect(Collectors.toMap(r -> ((Number) r[0]).longValue(), r -> ((Number) r[1]).doubleValue()));
        return zoneRepository.findAll().stream().map(zone -> {
            long spots = spotRepository.countByZone_idId(zone.getId());
            double bookedHours = hookedByZone.getOrDefault(zone.getId(), 0.0);
            double pct = spots > 0 ? Math.min(bookedHours * 100.0 / (spots * periodHours), 100.0) : 0;
            return new UtilizationResponse(zone.getId(), spots, bookedHours, pct);
        }).toList();
    }
}
