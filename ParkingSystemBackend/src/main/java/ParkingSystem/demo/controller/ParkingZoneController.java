package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.zone.ZoneRequest;
import ParkingSystem.demo.dto.zone.ZoneResponse;
import ParkingSystem.demo.service.ParkingZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zones")
@RequiredArgsConstructor
public class ParkingZoneController {

    private final ParkingZoneService zoneService;

    @GetMapping
    public ResponseEntity<List<ZoneResponse>> list() {
        return ResponseEntity.ok(zoneService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ZoneResponse> create(@Valid @RequestBody ZoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(zoneService.create(request.getLevel(), request.getType()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ZoneResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody ZoneRequest request) {
        return ResponseEntity.ok(zoneService.update(id, request.getLevel(), request.getType()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        zoneService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
