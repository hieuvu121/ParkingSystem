package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.recommendation.RecommendationResponse;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/spot")
    public ResponseEntity<RecommendationResponse> recommend(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        return recommendationService.recommend(lat, lng)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("No zones available"));
    }
}
