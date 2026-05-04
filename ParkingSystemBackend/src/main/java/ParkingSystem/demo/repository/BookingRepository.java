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
}
