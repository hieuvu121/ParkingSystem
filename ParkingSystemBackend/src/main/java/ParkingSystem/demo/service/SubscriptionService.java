package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.PageResponse;
import ParkingSystem.demo.dto.subscription.SubscriptionResponse;
import ParkingSystem.demo.entity.PackagesEntity;
import ParkingSystem.demo.entity.SubscriptionsEntity;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.exception.ConflictException;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.SubscriptionRepository;
import ParkingSystem.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PackageService packageService;
    private final UserRepository userRepository;

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

    // --- Admin methods ---

    public SubscriptionResponse adminGetUserSubscription(Long userId) {
        return subscriptionRepository.findActiveByUserId(userId, new Date())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription for user: " + userId));
    }

    @Transactional
    public SubscriptionResponse adminAddUserSubscription(Long userId, Long packageId) {
        if (subscriptionRepository.findActiveByUserId(userId, new Date()).isPresent()) {
            throw new ConflictException("User already has an active subscription");
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        PackagesEntity pkg = packageService.findOrThrow(packageId);
        Date start = new Date();
        Date end = Date.from(Instant.now().plus(pkg.getDurations(), ChronoUnit.DAYS));
        SubscriptionsEntity sub = SubscriptionsEntity.builder()
                .userId(user).packageName(pkg).startDate(start).endDate(end).price(pkg.getPrice()).build();
        return toResponse(subscriptionRepository.save(sub));
    }

    @Transactional
    public SubscriptionResponse adminChangeUserSubscription(Long userId, Long packageId) {
        SubscriptionsEntity sub = subscriptionRepository.findActiveByUserId(userId, new Date())
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription for user: " + userId));
        PackagesEntity pkg = packageService.findOrThrow(packageId);
        sub.setPackageName(pkg);
        sub.setPrice(pkg.getPrice());
        sub.setEndDate(Date.from(Instant.now().plus(pkg.getDurations(), ChronoUnit.DAYS)));
        return toResponse(subscriptionRepository.save(sub));
    }

    @Transactional
    public void adminRemoveUserSubscription(Long userId) {
        SubscriptionsEntity sub = subscriptionRepository.findActiveByUserId(userId, new Date())
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription for user: " + userId));
        subscriptionRepository.delete(sub);
    }

    public PageResponse<SubscriptionResponse> listAll(Pageable pageable) {
        return PageResponse.from(subscriptionRepository.findAll(pageable).map(this::toResponse));
    }

    private SubscriptionResponse toResponse(SubscriptionsEntity s) {
        return new SubscriptionResponse(s.getId(), s.getUserId().getId(),
                s.getPackageName().getId(), s.getPackageName().getName(),
                s.getStartDate(), s.getEndDate(), s.getPrice());
    }
}
