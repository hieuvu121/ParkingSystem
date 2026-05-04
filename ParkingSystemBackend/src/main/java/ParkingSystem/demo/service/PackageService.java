package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.pkg.PackageResponse;
import ParkingSystem.demo.entity.PackagesEntity;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.PackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PackageService {

    private final PackageRepository packageRepository;

    public List<PackageResponse> listAll() {
        return packageRepository.findAll().stream().map(this::toResponse).toList();
    }

    public PackageResponse create(String name, String description, Long durations, Long price) {
        PackagesEntity pkg = PackagesEntity.builder()
                .name(name).description(description).durations(durations).price(price).build();
        return toResponse(packageRepository.save(pkg));
    }

    public PackageResponse update(Long id, String name, String description, Long durations, Long price) {
        PackagesEntity pkg = findOrThrow(id);
        pkg.setName(name);
        pkg.setDescription(description);
        pkg.setDurations(durations);
        pkg.setPrice(price);
        return toResponse(packageRepository.save(pkg));
    }

    public void delete(Long id) {
        if (!packageRepository.existsById(id)) {
            throw new ResourceNotFoundException("Package not found with id: " + id);
        }
        packageRepository.deleteById(id);
    }

    public PackagesEntity findOrThrow(Long id) {
        return packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + id));
    }

    private PackageResponse toResponse(PackagesEntity p) {
        return new PackageResponse(p.getId(), p.getName(), p.getDescription(), p.getDurations(), p.getPrice());
    }
}
