package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.subscription.SubscriptionResponse;
import ParkingSystem.demo.entity.PackagesEntity;
import ParkingSystem.demo.entity.SubscriptionsEntity;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.exception.ConflictException;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PackageService packageService;

    public SubscriptionResponse subscribe(UserEntity user, Long packageId) {
        if (subscriptionRepository.findActiveByUserId(user.getId(), new Date()).isPresent()) {
            throw new ConflictException("User already has an active subscription");
        }
        PackagesEntity pkg = packageService.findOrThrow(packageId);
        Date start = new Date();
        Date end = Date.from(Instant.now().plus(pkg.getDurations(), ChronoUnit.DAYS));
        SubscriptionsEntity sub = SubscriptionsEntity.builder()
                .userId(user).packageName(pkg).startDate(start).endDate(end).price(pkg.getPrice()).build();
        return toResponse(subscriptionRepository.save(sub));
    }

    public SubscriptionResponse getActiveSubscription(Long userId) {
        return subscriptionRepository.findActiveByUserId(userId, new Date())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription for user: " + userId));
    }

    public List<SubscriptionResponse> listAll() {
        return subscriptionRepository.findAll().stream().map(this::toResponse).toList();
    }

    private SubscriptionResponse toResponse(SubscriptionsEntity s) {
        return new SubscriptionResponse(s.getId(), s.getUserId().getId(),
                s.getPackageName().getId(), s.getPackageName().getName(),
                s.getStartDate(), s.getEndDate(), s.getPrice());
    }
}
