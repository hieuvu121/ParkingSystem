package ParkingSystem.demo.dto.analytics;

public record UtilizationResponse(Long zoneId, long totalSpots, double bookedHours, double utilizationPercent) {}
