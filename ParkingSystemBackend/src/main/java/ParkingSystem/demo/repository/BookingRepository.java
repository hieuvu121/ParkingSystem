package ParkingSystem.demo.repository;

import ParkingSystem.demo.entity.BookingsEntity;
import ParkingSystem.demo.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<BookingsEntity, Long> {

    List<BookingsEntity> findByUserId_Id(Long userId);

    List<BookingsEntity> findByStatus(BookingStatus status);

    @Query("""
        SELECT b FROM BookingsEntity b
        WHERE b.spotId.id = :spotId
          AND b.status IN ('PENDING', 'APPROVED')
          AND b.startTime < :endTime
          AND b.endTime > :startTime
    """)
    List<BookingsEntity> findOverlapping(@Param("spotId") Long spotId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    @Query("""
        SELECT b FROM BookingsEntity b
        WHERE b.status = 'APPROVED' AND b.endTime < :now
    """)
    List<BookingsEntity> findExpired(@Param("now") LocalDateTime now);

    @Query(value = """
        SELECT EXTRACT(HOUR FROM b.start_time) AS hour, COUNT(*) AS cnt
        FROM bookings b
        WHERE b.status IN ('APPROVED', 'EXPIRED')
        GROUP BY EXTRACT(HOUR FROM b.start_time)
        ORDER BY hour
    """, nativeQuery = true)
    List<Object[]> countByHourOfDay();

    @Query(value = """
        SELECT ps.zone_id, COUNT(*) AS cnt
        FROM bookings b
        JOIN "parkingSpots" ps ON b.spot_id = ps.id
        WHERE b.status IN ('APPROVED', 'EXPIRED')
          AND b.start_time >= :from AND b.end_time <= :to
        GROUP BY ps.zone_id
    """, nativeQuery = true)
    List<Object[]> countByZoneInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT COUNT(*) FROM bookings b
        JOIN "parkingSpots" ps ON b.spot_id = ps.id
        WHERE ps.zone_id = :zoneId AND b.status IN ('APPROVED', 'EXPIRED')
    """, nativeQuery = true)
    long countByZoneId(@Param("zoneId") Long zoneId);

    @Query(value = """
        SELECT COUNT(*) FROM bookings b
        JOIN "parkingSpots" ps ON b.spot_id = ps.id
        WHERE ps.zone_id = :zoneId
          AND b.status IN ('APPROVED', 'EXPIRED')
          AND EXTRACT(DOW FROM b.start_time) = :pgDow
          AND EXTRACT(HOUR FROM b.start_time) = :hour
    """, nativeQuery = true)
    long countHistoricalBookings(@Param("zoneId") Long zoneId,
                                  @Param("pgDow") int pgDow,
                                  @Param("hour") int hour);
}
