package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.subscription.SubscriptionRequest;
import ParkingSystem.demo.dto.subscription.SubscriptionResponse;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<List<SubscriptionResponse>> listAll() {
        return ResponseEntity.ok(subscriptionService.listAll());
    }
}
