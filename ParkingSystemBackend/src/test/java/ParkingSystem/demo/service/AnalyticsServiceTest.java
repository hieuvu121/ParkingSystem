package ParkingSystem.demo.service;

import ParkingSystem.demo.entity.ParkingZonesEntity;
import ParkingSystem.demo.repository.BookingRepository;
import ParkingSystem.demo.repository.ParkingSpotRepository;
import ParkingSystem.demo.repository.ParkingZoneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private ParkingSpotRepository spotRepository;
    @Mock private ParkingZoneRepository zoneRepository;
    @InjectMocks private AnalyticsService analyticsService;

    private ParkingZonesEntity zone(Long id) {
        return ParkingZonesEntity.builder().id(id).level(1L).type("INDOOR").build();
    }

    private List<Object[]> objectRows(Object[]... rows) {
        List<Object[]> list = new ArrayList<>();
        for (Object[] row : rows) list.add(row);
        return list;
    }

    @Test
    void getOccupancy_noSpots_returnsEmpty() {
        when(spotRepository.count()).thenReturn(0L);
        var from = LocalDateTime.now().minusDays(1);
        var to = LocalDateTime.now();

        assertThat(analyticsService.getOccupancy(from, to)).isEmpty();
    }

    @Test
    void getOccupancy_withData_returnsOccupancyPercent() {
        var from = LocalDateTime.now().minusDays(1);
        var to = LocalDateTime.now();
        when(spotRepository.count()).thenReturn(10L);
        when(bookingRepository.countByZoneInRange(from, to)).thenReturn(objectRows(new Object[]{1L, 5L}));
        when(spotRepository.countByZone_idId(1L)).thenReturn(10L);

        var result = analyticsService.getOccupancy(from, to);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).occupancyPercent()).isEqualTo(50.0);
    }

    @Test
    void getPeakHours_withData_normalisesToMaxHour() {
        when(bookingRepository.countByHourOfDay())
                .thenReturn(objectRows(new Object[]{8, 10L}, new Object[]{9, 5L}));

        var result = analyticsService.getPeakHours();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).averageOccupancyPercent()).isEqualTo(100.0);
        assertThat(result.get(1).averageOccupancyPercent()).isEqualTo(50.0);
    }

    @Test
    void getPeakHours_noData_returnsEmpty() {
        when(bookingRepository.countByHourOfDay()).thenReturn(new ArrayList<>());

        assertThat(analyticsService.getPeakHours()).isEmpty();
    }

    @Test
    void getUtilization_returnsPerZoneStats() {
        when(zoneRepository.findAll()).thenReturn(List.of(zone(1L)));
        when(spotRepository.countByZone_idId(1L)).thenReturn(10L);
        when(bookingRepository.countByZoneId(1L)).thenReturn(3L);

        var result = analyticsService.getUtilization();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).utilizationPercent()).isEqualTo(30.0);
    }

    @Test
    void getUtilization_noSpots_returnsZeroPercent() {
        when(zoneRepository.findAll()).thenReturn(List.of(zone(1L)));
        when(spotRepository.countByZone_idId(1L)).thenReturn(0L);
        when(bookingRepository.countByZoneId(1L)).thenReturn(0L);

        var result = analyticsService.getUtilization();

        assertThat(result.get(0).utilizationPercent()).isEqualTo(0.0);
    }
}
