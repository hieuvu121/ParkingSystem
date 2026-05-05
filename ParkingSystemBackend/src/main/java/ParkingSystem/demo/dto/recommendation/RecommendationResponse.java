package ParkingSystem.demo.dto.recommendation;

public record RecommendationResponse(Long zoneId, String reason, long availableSpots, double predictedProbability) {}
