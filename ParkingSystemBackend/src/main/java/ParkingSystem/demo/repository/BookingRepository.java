package ParkingSystem.demo.repository;

import ParkingSystem.demo.entity.BookingsEntity;
import ParkingSystem.demo.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
          AND b.status IN (ParkingSystem.demo.enums.BookingStatus.PENDING, ParkingSystem.demo.enums.BookingStatus.APPROVED)
          AND b.startTime < :endTime
          AND b.endTime > :startTime
    """)
    List<BookingsEntity> findOverlapping(@Param("spotId") Long spotId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    @Query("""
        SELECT b FROM BookingsEntity b
        WHERE b.status = ParkingSystem.demo.enums.BookingStatus.APPROVED AND b.endTime < :now
    """)
    List<BookingsEntity> findExpired(@Param("now") LocalDateTime now);

    @Modifying
    @Query("""
        UPDATE BookingsEntity b SET b.status = ParkingSystem.demo.enums.BookingStatus.EXPIRED
        WHERE b.status = ParkingSystem.demo.enums.BookingStatus.APPROVED AND b.endTime < :now
    """)
    int bulkExpire(@Param("now") LocalDateTime now);

    // Native queries use ordinal integers: PENDING=0, APPROVED=1, EXPIRED=2, CANCELLED=3
    @Query(value = """
        SELECT EXTRACT(HOUR FROM b.start_time) AS hour, COUNT(*) AS cnt
        FROM bookings b
        WHERE b.status IN (1, 2)
        GROUP BY EXTRACT(HOUR FROM b.start_time)
        ORDER BY hour
    """, nativeQuery = true)
    List<Object[]> countByHourOfDay();

    @Query(value = """
        SELECT ps.zone_id, COUNT(*) AS cnt
        FROM bookings b
        JOIN parking_spots ps ON b.spot_id = ps.id
        WHERE b.status IN (1, 2)
          AND b.start_time >= :from AND b.end_time <= :to
        GROUP BY ps.zone_id
    """, nativeQuery = true)
    List<Object[]> countByZoneInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT ps.zone_id,
               SUM(EXTRACT(EPOCH FROM (
                   LEAST(b.end_time, CAST(:to AS timestamp))
                   - GREATEST(b.start_time, CAST(:from AS timestamp))
               )) / 3600.0) AS booked_hours
        FROM bookings b
        JOIN parking_spots ps ON b.spot_id = ps.id
        WHERE b.status IN (1, 2)
          AND b.start_time < :to
          AND b.end_time > :from
        GROUP BY ps.zone_id
    """, nativeQuery = true)
    List<Object[]> bookedHoursByZoneInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT COUNT(*) FROM bookings b
        JOIN parking_spots ps ON b.spot_id = ps.id
        WHERE ps.zone_id = :zoneId AND b.status IN (1, 2)
    """, nativeQuery = true)
    long countByZoneId(@Param("zoneId") Long zoneId);

    @Query(value = """
        SELECT COUNT(*) FROM bookings b
        JOIN parking_spots ps ON b.spot_id = ps.id
        WHERE ps.zone_id = :zoneId
          AND b.status IN (1, 2)
          AND b.start_time >= :from
          AND b.start_time <= :to
          AND EXTRACT(DOW FROM b.start_time) = :pgDow
          AND EXTRACT(HOUR FROM b.start_time) = :hour
    """, nativeQuery = true)
    long countHistoricalBookings(@Param("zoneId") Long zoneId,
                                  @Param("pgDow") int pgDow,
                                  @Param("hour") int hour,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to);

    long countByUserId_IdAndStatus(Long userId, BookingStatus status);

    @Query(value = """
        SELECT b FROM BookingsEntity b
        WHERE (:status IS NULL OR b.status = :status)
          AND b.startTime >= :from
          AND b.startTime <= :to
        ORDER BY b.startTime DESC
        """,
        countQuery = """
        SELECT COUNT(b) FROM BookingsEntity b
        WHERE (:status IS NULL OR b.status = :status)
          AND b.startTime >= :from
          AND b.startTime <= :to
        """)
    Page<BookingsEntity> findAdminBookings(@Param("status") BookingStatus status,
                                           @Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to,
                                           Pageable pageable);

    @Query("""
        SELECT b FROM BookingsEntity b
        WHERE b.userId.id = :userId
          AND (:statusFilter = false OR b.status IN :statuses)
          AND (:cursorEnabled = false
               OR b.startTime < :cursorTime
               OR (b.startTime = :cursorTime AND b.id < :cursorId))
        ORDER BY b.startTime DESC, b.id DESC
    """)
    List<BookingsEntity> findUserBookingsDesc(@Param("userId") Long userId,
                                              @Param("statuses") List<BookingStatus> statuses,
                                              @Param("statusFilter") boolean statusFilter,
                                              @Param("cursorEnabled") boolean cursorEnabled,
                                              @Param("cursorTime") LocalDateTime cursorTime,
                                              @Param("cursorId") Long cursorId,
                                              Pageable pageable);

    @Query("""
        SELECT b FROM BookingsEntity b
        WHERE b.userId.id = :userId
          AND (:statusFilter = false OR b.status IN :statuses)
          AND (:cursorEnabled = false
               OR b.startTime > :cursorTime
               OR (b.startTime = :cursorTime AND b.id > :cursorId))
        ORDER BY b.startTime ASC, b.id ASC
    """)
    List<BookingsEntity> findUserBookingsAsc(@Param("userId") Long userId,
                                             @Param("statuses") List<BookingStatus> statuses,
                                             @Param("statusFilter") boolean statusFilter,
                                             @Param("cursorEnabled") boolean cursorEnabled,
                                             @Param("cursorTime") LocalDateTime cursorTime,
                                             @Param("cursorId") Long cursorId,
                                             Pageable pageable);
}
