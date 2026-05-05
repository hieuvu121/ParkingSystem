package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.recommendation.RecommendationResponse;
import ParkingSystem.demo.entity.ParkingZonesEntity;
import ParkingSystem.demo.enums.SpotStatus;
import ParkingSystem.demo.repository.ParkingSpotRepository;
import ParkingSystem.demo.repository.ParkingZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ParkingZoneRepository zoneRepository;
    private final ParkingSpotRepository spotRepository;
    private final PredictionService predictionService;

    public Optional<RecommendationResponse> recommend(Double userLat, Double userLng) {
        List<ParkingZonesEntity> zones = zoneRepository.findAll();
        LocalDateTime in30 = LocalDateTime.now().plusMinutes(30);

        if (userLat != null && userLng != null) {
            Optional<ParkingZonesEntity> nearest = zones.stream()
                    .filter(z -> z.getLat() != null && z.getLng() != null)
                    .filter(z -> spotRepository.countByZone_idIdAndStatus(z.getId(), SpotStatus.AVAILABLE) > 0)
                    .min(Comparator.comparingDouble(z -> haversine(userLat, userLng, z.getLat(), z.getLng())));
            if (nearest.isPresent()) {
                ParkingZonesEntity z = nearest.get();
                long avail = spotRepository.countByZone_idIdAndStatus(z.getId(), SpotStatus.AVAILABLE);
                double prob = predictionService.predict(z.getId(), in30).availabilityProbability();
                return Optional.of(new RecommendationResponse(z.getId(), "Nearest available zone", avail, prob));
            }
        }

        Optional<ParkingZonesEntity> leastCongested = zones.stream()
                .filter(z -> spotRepository.countByZone_idIdAndStatus(z.getId(), SpotStatus.AVAILABLE) > 0)
                .max(Comparator.comparingLong(z ->
                        spotRepository.countByZone_idIdAndStatus(z.getId(), SpotStatus.AVAILABLE)));
        if (leastCongested.isPresent()) {
            ParkingZonesEntity z = leastCongested.get();
            long avail = spotRepository.countByZone_idIdAndStatus(z.getId(), SpotStatus.AVAILABLE);
            double prob = predictionService.predict(z.getId(), in30).availabilityProbability();
            return Optional.of(new RecommendationResponse(z.getId(), "Least congested zone", avail, prob));
        }

        return zones.stream()
                .max(Comparator.comparingDouble(z ->
                        predictionService.predict(z.getId(), in30).availabilityProbability()))
                .map(z -> {
                    long avail = spotRepository.countByZone_idIdAndStatus(z.getId(), SpotStatus.AVAILABLE);
                    double prob = predictionService.predict(z.getId(), in30).availabilityProbability();
                    return new RecommendationResponse(z.getId(), "Best predicted availability", avail, prob);
                });
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
