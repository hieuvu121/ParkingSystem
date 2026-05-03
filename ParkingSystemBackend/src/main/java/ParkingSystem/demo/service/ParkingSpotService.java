package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.spot.DashboardResponse;
import ParkingSystem.demo.dto.spot.SpotResponse;
import ParkingSystem.demo.dto.spot.ZoneSummary;
import ParkingSystem.demo.entity.ParkingSpotsEntity;
import ParkingSystem.demo.entity.ParkingZonesEntity;
import ParkingSystem.demo.enums.SpotStatus;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.ParkingSpotRepository;
import ParkingSystem.demo.repository.ParkingZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ParkingSpotService {

    private final ParkingSpotRepository spotRepository;
    private final ParkingZoneService zoneService;
    private final RealtimeService realtimeService;
    private final ParkingZoneRepository zoneRepository;

    public List<SpotResponse> listByZone(Long zoneId) {
        return spotRepository.findByZone_idId(zoneId).stream().map(this::toResponse).toList();
    }

    public SpotResponse create(Long zoneId, Long row, Long col, String type) {
        ParkingZonesEntity zone = zoneService.findOrThrow(zoneId);
        ParkingSpotsEntity spot = ParkingSpotsEntity.builder()
                .row(row).col(col).type(type).status(SpotStatus.AVAILABLE).zone_id(zone).build();
        return toResponse(spotRepository.save(spot));
    }

    public SpotResponse update(Long spotId, Long row, Long col, String type) {
        ParkingSpotsEntity spot = findOrThrow(spotId);
        spot.setRow(row);
        spot.setCol(col);
        spot.setType(type);
        return toResponse(spotRepository.save(spot));
    }

    public void delete(Long spotId) {
        if (!spotRepository.existsById(spotId)) {
            throw new ResourceNotFoundException("Spot not found with id: " + spotId);
        }
        spotRepository.deleteById(spotId);
    }

    public void updateStatus(Long spotId, SpotStatus status) {
        ParkingSpotsEntity spot = findOrThrow(spotId);
        spot.setStatus(status);
        ParkingSpotsEntity saved = spotRepository.save(spot);
        realtimeService.broadcastSpotUpdate(saved);
        realtimeService.broadcastDashboard();
    }

    public DashboardResponse getDashboard() {
        long total = spotRepository.count();
        long available = spotRepository.countByStatus(SpotStatus.AVAILABLE);
        long occupied = spotRepository.countByStatus(SpotStatus.OCCUPIED);
        List<ZoneSummary> byZone = zoneRepository.findAll().stream().map(z -> {
            long zTotal = spotRepository.countByZone_idId(z.getId());
            long zAvail = spotRepository.countByZone_idIdAndStatus(z.getId(), SpotStatus.AVAILABLE);
            long zOccupied = spotRepository.countByZone_idIdAndStatus(z.getId(), SpotStatus.OCCUPIED);
            return new ZoneSummary(z.getId(), zTotal, zAvail, zOccupied);
        }).toList();
        return new DashboardResponse(total, available, occupied, byZone);
    }

    public void simulate(Integer count) {
        List<ParkingSpotsEntity> spots = spotRepository.findAll();
        if (spots.isEmpty()) return;
        Random rng = new Random();
        int limit = (count != null) ? Math.min(count, spots.size()) : spots.size();
        spots.stream().limit(limit).forEach(spot -> {
            SpotStatus newStatus = rng.nextBoolean() ? SpotStatus.OCCUPIED : SpotStatus.AVAILABLE;
            spot.setStatus(newStatus);
            ParkingSpotsEntity saved = spotRepository.save(spot);
            realtimeService.broadcastSpotUpdate(saved);
        });
        realtimeService.broadcastDashboard();
    }

    public ParkingSpotsEntity findOrThrow(Long id) {
        return spotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spot not found with id: " + id));
    }

    public SpotResponse toResponse(ParkingSpotsEntity s) {
        return new SpotResponse(s.getId(), s.getRow(), s.getCol(), s.getType(),
                s.getStatus(), s.getZone_id().getId());
    }
}
