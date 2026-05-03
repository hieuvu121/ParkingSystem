package ParkingSystem.demo.repository;

import ParkingSystem.demo.entity.ParkingSpotsEntity;
import ParkingSystem.demo.enums.SpotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpotsEntity, Long> {

    @Query("SELECT s FROM ParkingSpotsEntity s WHERE s.zone_id.id = :zoneId")
    List<ParkingSpotsEntity> findByZone_idId(@Param("zoneId") Long zoneId);

    long countByStatus(SpotStatus status);

    @Query("SELECT COUNT(s) FROM ParkingSpotsEntity s WHERE s.zone_id.id = :zoneId AND s.status = :status")
    long countByZone_idIdAndStatus(@Param("zoneId") Long zoneId, @Param("status") SpotStatus status);

    @Query("SELECT COUNT(s) FROM ParkingSpotsEntity s WHERE s.zone_id.id = :zoneId")
    long countByZone_idId(@Param("zoneId") Long zoneId);
}
