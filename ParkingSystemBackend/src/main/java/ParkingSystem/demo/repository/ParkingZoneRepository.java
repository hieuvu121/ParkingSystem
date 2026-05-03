package ParkingSystem.demo.repository;

import ParkingSystem.demo.entity.ParkingZonesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParkingZoneRepository extends JpaRepository<ParkingZonesEntity, Long> {}
