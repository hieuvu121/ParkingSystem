package ParkingSystem.demo.service;

import ParkingSystem.demo.entity.PackagesEntity;
import ParkingSystem.demo.entity.SubscriptionsEntity;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.exception.ConflictException;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private PackageService packageService;
    @InjectMocks private SubscriptionService subscriptionService;

    private UserEntity user() {
        UserEntity u = new UserEntity();
        u.setId(1L);
        u.setEmail("user@test.com");
        return u;
    }

    private PackagesEntity pkg() {
        return PackagesEntity.builder().id(1L).name("Monthly").description("desc")
                .durations(30L).price(500000L).build();
    }

    private SubscriptionsEntity sub(UserEntity user, PackagesEntity pkg) {
        return SubscriptionsEntity.builder()
                .id(1L).userId(user).packageName(pkg)
                .startDate(new Date()).endDate(new Date())
                .price(pkg.getPrice()).build();
    }

    @Test
    void subscribe_noExistingSub_createsSub() {
        UserEntity user = user();
        PackagesEntity pkg = pkg();
        when(subscriptionRepository.findActiveByUserId(eq(1L), any(Date.class))).thenReturn(Optional.empty());
        when(packageService.findOrThrow(1L)).thenReturn(pkg);
        when(subscriptionRepository.save(any())).thenAnswer(i -> sub(user, pkg));

        var result = subscriptionService.subscribe(user, 1L);

        assertThat(result.packageName()).isEqualTo("Monthly");
        assertThat(result.price()).isEqualTo(500000L);
    }

    @Test
    void subscribe_alreadyHasActiveSub_throwsConflict() {
        UserEntity user = user();
        PackagesEntity pkg = pkg();
        when(subscriptionRepository.findActiveByUserId(eq(1L), any(Date.class)))
                .thenReturn(Optional.of(sub(user, pkg)));

        assertThatThrownBy(() -> subscriptionService.subscribe(user, 1L))
                .isInstanceOf(ConflictException.class);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void getActiveSubscription_activeExists_returnsResponse() {
        UserEntity user = user();
        PackagesEntity pkg = pkg();
        when(subscriptionRepository.findActiveByUserId(eq(1L), any(Date.class)))
                .thenReturn(Optional.of(sub(user, pkg)));

        var result = subscriptionService.getActiveSubscription(1L);

        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.packageName()).isEqualTo("Monthly");
    }

    @Test
    void getActiveSubscription_noneActive_throwsNotFound() {
        when(subscriptionRepository.findActiveByUserId(eq(99L), any(Date.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.getActiveSubscription(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listAll_returnsAllSubscriptions() {
        UserEntity user = user();
        PackagesEntity pkg = pkg();
        when(subscriptionRepository.findAll()).thenReturn(List.of(sub(user, pkg)));

        assertThat(subscriptionService.listAll()).hasSize(1);
    }
}
