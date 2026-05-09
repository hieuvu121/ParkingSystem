package ParkingSystem.demo.service;

import ParkingSystem.demo.repository.BookingRepository;
import ParkingSystem.demo.repository.ParkingSpotRepository;
import ParkingSystem.demo.repository.ParkingZoneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PredictionServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private ParkingSpotRepository spotRepository;
    @Mock private ParkingZoneRepository zoneRepository;
    @InjectMocks private PredictionService predictionService;

    @Test
    void predict_noSpots_returnsZeroProbability() {
        when(spotRepository.countByZone_idId(1L)).thenReturn(0L);
        var result = predictionService.predict(1L, LocalDateTime.now());
        assertThat(result.availabilityProbability()).isEqualTo(0.0);
    }

    @Test
    void predict_allSpotsHistoricallyBooked_returnsZero() {
        LocalDateTime target = LocalDateTime.of(2026, 5, 4, 9, 0);
        when(spotRepository.countByZone_idId(1L)).thenReturn(10L);
        when(bookingRepository.countHistoricalBookings(eq(1L), anyInt(), eq(9))).thenReturn(10L);

        var result = predictionService.predict(1L, target);

        assertThat(result.availabilityProbability()).isEqualTo(0.0);
    }

    @Test
    void predict_noHistoricalBookings_returnsOne() {
        LocalDateTime target = LocalDateTime.of(2026, 5, 4, 9, 0);
        when(spotRepository.countByZone_idId(1L)).thenReturn(10L);
        when(bookingRepository.countHistoricalBookings(eq(1L), anyInt(), eq(9))).thenReturn(0L);

        var result = predictionService.predict(1L, target);

        assertThat(result.availabilityProbability()).isEqualTo(1.0);
    }

    @Test
    void predict_halfBooked_returnsHalfProbability() {
        LocalDateTime target = LocalDateTime.of(2026, 5, 4, 9, 0);
        when(spotRepository.countByZone_idId(1L)).thenReturn(10L);
        when(bookingRepository.countHistoricalBookings(eq(1L), anyInt(), eq(9))).thenReturn(5L);

        var result = predictionService.predict(1L, target);

        assertThat(result.availabilityProbability()).isEqualTo(0.5);
    }

    @Test
    void predict_historicalExceedsTotalSpots_clampsToZero() {
        LocalDateTime target = LocalDateTime.of(2026, 5, 4, 9, 0);
        when(spotRepository.countByZone_idId(1L)).thenReturn(10L);
        when(bookingRepository.countHistoricalBookings(eq(1L), anyInt(), eq(9))).thenReturn(999L);

        var result = predictionService.predict(1L, target);

        assertThat(result.availabilityProbability()).isEqualTo(0.0);
    }
}
