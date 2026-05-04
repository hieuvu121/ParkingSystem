package ParkingSystem.demo.repository;

import ParkingSystem.demo.entity.PackagesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PackageRepository extends JpaRepository<PackagesEntity, Long> {
    Optional<PackagesEntity> findByName(String name);
}
