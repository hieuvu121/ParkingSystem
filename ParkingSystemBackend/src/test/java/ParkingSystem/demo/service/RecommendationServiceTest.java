package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.prediction.AvailabilityPredictionResponse;
import ParkingSystem.demo.entity.ParkingZonesEntity;
import ParkingSystem.demo.enums.SpotStatus;
import ParkingSystem.demo.repository.ParkingSpotRepository;
import ParkingSystem.demo.repository.ParkingZoneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock private ParkingZoneRepository zoneRepository;
    @Mock private ParkingSpotRepository spotRepository;
    @Mock private PredictionService predictionService;
    @InjectMocks private RecommendationService recommendationService;

    private ParkingZonesEntity zone(Long id, Double lat, Double lng) {
        return ParkingZonesEntity.builder().id(id).level(1L).type("INDOOR").lat(lat).lng(lng).build();
    }

    private AvailabilityPredictionResponse prediction(Long zoneId, double prob) {
        return new AvailabilityPredictionResponse(zoneId, "2026-05-04T09:00", prob);
    }

    @Test
    void recommend_noZones_returnsEmpty() {
        when(zoneRepository.findAll()).thenReturn(List.of());
        assertThat(recommendationService.recommend(null, null)).isEmpty();
    }

    @Test
    void recommend_byProximity_returnsNearestAvailableZone() {
        ParkingZonesEntity near = zone(1L, 10.0, 106.0);
        ParkingZonesEntity far = zone(2L, 20.0, 107.0);
        when(zoneRepository.findAll()).thenReturn(List.of(near, far));
        when(spotRepository.countByZone_idIdAndStatus(1L, SpotStatus.AVAILABLE)).thenReturn(5L);
        when(spotRepository.countByZone_idIdAndStatus(2L, SpotStatus.AVAILABLE)).thenReturn(3L);
        when(predictionService.predict(eq(1L), any())).thenReturn(prediction(1L, 0.8));

        var result = recommendationService.recommend(10.0, 106.0);

        assertThat(result).isPresent();
        assertThat(result.get().zoneId()).isEqualTo(1L);
        assertThat(result.get().reason()).isEqualTo("Nearest available zone");
    }

    @Test
    void recommend_byCongestion_returnsLeastCongestedZone() {
        ParkingZonesEntity zone1 = zone(1L, null, null);
        ParkingZonesEntity zone2 = zone(2L, null, null);
        when(zoneRepository.findAll()).thenReturn(List.of(zone1, zone2));
        when(spotRepository.countByZone_idIdAndStatus(1L, SpotStatus.AVAILABLE)).thenReturn(10L);
        when(spotRepository.countByZone_idIdAndStatus(2L, SpotStatus.AVAILABLE)).thenReturn(2L);
        when(predictionService.predict(eq(1L), any())).thenReturn(prediction(1L, 0.9));

        var result = recommendationService.recommend(null, null);

        assertThat(result).isPresent();
        assertThat(result.get().zoneId()).isEqualTo(1L);
        assertThat(result.get().reason()).isEqualTo("Least congested zone");
    }

    @Test
    void recommend_allFull_fallsBackToPrediction() {
        ParkingZonesEntity zone1 = zone(1L, null, null);
        when(zoneRepository.findAll()).thenReturn(List.of(zone1));
        when(spotRepository.countByZone_idIdAndStatus(1L, SpotStatus.AVAILABLE)).thenReturn(0L);
        when(predictionService.predict(eq(1L), any())).thenReturn(prediction(1L, 0.6));

        var result = recommendationService.recommend(null, null);

        assertThat(result).isPresent();
        assertThat(result.get().reason()).isEqualTo("Best predicted availability");
    }
}
