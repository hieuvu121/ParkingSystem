package ParkingSystem.demo.dto.prediction;

public record AvailabilityPredictionResponse(Long zoneId, String targetTime, double availabilityProbability) {}
