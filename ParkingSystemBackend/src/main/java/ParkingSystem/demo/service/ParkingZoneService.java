package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.zone.ZoneResponse;
import ParkingSystem.demo.entity.ParkingZonesEntity;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.ParkingZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParkingZoneService {

    private final ParkingZoneRepository zoneRepository;

    public List<ZoneResponse> listAll() {
        return zoneRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ZoneResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public ZoneResponse create(Long level, String type) {
        ParkingZonesEntity zone = ParkingZonesEntity.builder().level(level).type(type).build();
        return toResponse(zoneRepository.save(zone));
    }

    public ZoneResponse update(Long id, Long level, String type) {
        ParkingZonesEntity zone = findOrThrow(id);
        zone.setLevel(level);
        zone.setType(type);
        return toResponse(zoneRepository.save(zone));
    }

    public void delete(Long id) {
        if (!zoneRepository.existsById(id)) {
            throw new ResourceNotFoundException("Zone not found with id: " + id);
        }
        zoneRepository.deleteById(id);
    }

    public ParkingZonesEntity findOrThrow(Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with id: " + id));
    }

    private ZoneResponse toResponse(ParkingZonesEntity z) {
        return new ZoneResponse(z.getId(), z.getLevel(), z.getType());
    }
}
