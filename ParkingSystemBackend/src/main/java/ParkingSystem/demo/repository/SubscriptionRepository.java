package ParkingSystem.demo.repository;

import ParkingSystem.demo.entity.SubscriptionsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionsEntity, Long> {

    List<SubscriptionsEntity> findByUserId_Id(Long userId);

    @Query("SELECT s FROM SubscriptionsEntity s WHERE s.userId.id = :userId AND s.endDate >= :now")
    Optional<SubscriptionsEntity> findActiveByUserId(@Param("userId") Long userId, @Param("now") Date now);
}
