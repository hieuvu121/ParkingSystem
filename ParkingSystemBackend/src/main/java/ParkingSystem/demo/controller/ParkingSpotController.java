package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.spot.*;
import ParkingSystem.demo.service.ParkingSpotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ParkingSpotController {

    private final ParkingSpotService spotService;

    @GetMapping("/api/zones/{zoneId}/spots")
    public ResponseEntity<List<SpotResponse>> listByZone(@PathVariable Long zoneId) {
        return ResponseEntity.ok(spotService.listByZone(zoneId));
    }

    @PostMapping("/api/zones/{zoneId}/spots")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SpotResponse> create(@PathVariable Long zoneId,
                                               @Valid @RequestBody SpotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(spotService.create(zoneId, request.getRow(), request.getCol(), request.getType()));
    }

    @PutMapping("/api/spots/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SpotResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody SpotRequest request) {
        return ResponseEntity.ok(spotService.update(id, request.getRow(), request.getCol(), request.getType()));
    }

    @DeleteMapping("/api/spots/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        spotService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/spots/dashboard")
    public ResponseEntity<DashboardResponse> dashboard() {
        return ResponseEntity.ok(spotService.getDashboard());
    }

    @PostMapping("/api/spots/webhook")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> webhook(@Valid @RequestBody SpotStatusUpdateRequest request) {
        spotService.updateStatus(request.getSpotId(), request.getStatus());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/spots/simulate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> simulate(@RequestParam(required = false) Integer count) {
        spotService.simulate(count);
        return ResponseEntity.ok().build();
    }
}
