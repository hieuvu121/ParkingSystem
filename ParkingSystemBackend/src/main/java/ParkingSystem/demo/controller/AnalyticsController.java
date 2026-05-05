package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.analytics.OccupancyResponse;
import ParkingSystem.demo.dto.analytics.PeakHourResponse;
import ParkingSystem.demo.dto.analytics.UtilizationResponse;
import ParkingSystem.demo.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/occupancy")
    public ResponseEntity<List<OccupancyResponse>> occupancy(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(analyticsService.getOccupancy(from, to));
    }

    @GetMapping("/peak-hours")
    public ResponseEntity<List<PeakHourResponse>> peakHours() {
        return ResponseEntity.ok(analyticsService.getPeakHours());
    }

    @GetMapping("/utilization")
    public ResponseEntity<List<UtilizationResponse>> utilization() {
        return ResponseEntity.ok(analyticsService.getUtilization());
    }
}
