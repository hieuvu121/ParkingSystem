package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.pkg.PackageRequest;
import ParkingSystem.demo.dto.pkg.PackageResponse;
import ParkingSystem.demo.service.PackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    @GetMapping
    public ResponseEntity<List<PackageResponse>> list() {
        return ResponseEntity.ok(packageService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PackageResponse> create(@Valid @RequestBody PackageRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(packageService.create(req.getName(), req.getDescription(), req.getDurations(), req.getPrice()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PackageResponse> update(@PathVariable Long id, @Valid @RequestBody PackageRequest req) {
        return ResponseEntity.ok(packageService.update(id, req.getName(), req.getDescription(), req.getDurations(), req.getPrice()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        packageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
