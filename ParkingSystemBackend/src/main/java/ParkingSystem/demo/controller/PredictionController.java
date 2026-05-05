package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.prediction.AvailabilityPredictionResponse;
import ParkingSystem.demo.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/predict")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    @GetMapping("/availability")
    public ResponseEntity<AvailabilityPredictionResponse> predict(
            @RequestParam Long zoneId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime targetTime) {
        return ResponseEntity.ok(predictionService.predict(zoneId, targetTime));
    }
}
