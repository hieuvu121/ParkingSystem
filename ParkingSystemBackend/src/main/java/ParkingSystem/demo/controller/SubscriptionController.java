package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.PageResponse;
import ParkingSystem.demo.dto.subscription.SubscriptionRequest;
import ParkingSystem.demo.dto.subscription.SubscriptionResponse;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/subscriptions")
    @PreAuthorize("hasRole('USERS')")
    public ResponseEntity<SubscriptionResponse> subscribe(@AuthenticationPrincipal UserEntity user,
                                                          @Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.subscribe(user, request.getPackageId()));
    }

    @GetMapping("/subscriptions/my")
    @PreAuthorize("hasRole('USERS')")
    public ResponseEntity<SubscriptionResponse> mySubscription(@AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(subscriptionService.getActiveSubscription(user.getId()));
    }

    @GetMapping("/admin/subscriptions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<SubscriptionResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(subscriptionService.listAll(PageRequest.of(page, size)));
    }

    @GetMapping("/admin/users/{userId}/subscription")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionResponse> getUserSubscription(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.adminGetUserSubscription(userId));
    }

    @PostMapping("/admin/users/{userId}/subscription")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionResponse> addUserSubscription(@PathVariable Long userId,
                                                                     @Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.adminAddUserSubscription(userId, request.getPackageId()));
    }

    @PutMapping("/admin/users/{userId}/subscription")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriptionResponse> changeUserSubscription(@PathVariable Long userId,
                                                                        @Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionService.adminChangeUserSubscription(userId, request.getPackageId()));
    }

    @DeleteMapping("/admin/users/{userId}/subscription")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeUserSubscription(@PathVariable Long userId) {
        subscriptionService.adminRemoveUserSubscription(userId);
        return ResponseEntity.noContent().build();
    }
}
