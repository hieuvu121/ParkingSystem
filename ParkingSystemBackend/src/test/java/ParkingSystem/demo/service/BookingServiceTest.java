package ParkingSystem.demo.service;

import ParkingSystem.demo.entity.*;
import ParkingSystem.demo.enums.BookingStatus;
import ParkingSystem.demo.enums.Role;
import ParkingSystem.demo.enums.SpotStatus;
import ParkingSystem.demo.exception.ConflictException;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private ParkingSpotService spotService;
    @InjectMocks private BookingService bookingService;

    private UserEntity user() {
        return UserEntity.builder().id(1L).fullName("Alice").email("a@x.com")
                .password("pw").role(Role.USERS).isActive(true).build();
    }

    private ParkingSpotsEntity spot() {
        var zone = ParkingZonesEntity.builder().id(1L).level(1L).type("STD").build();
        return ParkingSpotsEntity.builder().id(1L).row(1L).col(1L).type("CAR")
                .status(SpotStatus.AVAILABLE).zone_id(zone).build();
    }

    private LocalDateTime now() { return LocalDateTime.now(); }

    @Test
    void create_withAvailableSpot_createsBooking() {
        when(spotService.findOrThrow(1L)).thenReturn(spot());
        when(bookingRepository.findOverlapping(eq(1L), any(), any())).thenReturn(List.of());
        when(bookingRepository.save(any())).thenAnswer(i -> {
            var b = (BookingsEntity) i.getArgument(0);
            b = BookingsEntity.builder().id(10L).startTime(b.getStartTime())
                    .endTime(b.getEndTime()).status(b.getStatus())
                    .userId(b.getUserId()).spotId(b.getSpotId()).build();
            return b;
        });
        var result = bookingService.create(user(), 1L, now(), now().plusHours(2));
        assertThat(result.status()).isEqualTo(BookingStatus.APPROVED);
        verify(spotService).updateStatus(1L, SpotStatus.OCCUPIED);
    }

    @Test
    void create_withOverlappingBooking_throwsConflict() {
        when(spotService.findOrThrow(1L)).thenReturn(spot());
        var existing = BookingsEntity.builder().id(5L).status(BookingStatus.APPROVED)
                .startTime(now()).endTime(now().plusHours(1))
                .userId(user()).spotId(spot()).build();
        when(bookingRepository.findOverlapping(eq(1L), any(), any())).thenReturn(List.of(existing));
        assertThatThrownBy(() -> bookingService.create(user(), 1L, now(), now().plusHours(2)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cancel_approvedBooking_cancelsAndFreesSpot() {
        var b = BookingsEntity.builder().id(1L).status(BookingStatus.APPROVED)
                .startTime(now()).endTime(now().plusHours(1))
                .userId(user()).spotId(spot()).build();
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(b));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        bookingService.cancel(1L, 1L);
        verify(bookingRepository).save(argThat(bk -> bk.getStatus() == BookingStatus.CANCELLED));
        verify(spotService).updateStatus(1L, SpotStatus.AVAILABLE);
    }

    @Test
    void cancel_unknownBooking_throwsNotFound() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> bookingService.cancel(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
