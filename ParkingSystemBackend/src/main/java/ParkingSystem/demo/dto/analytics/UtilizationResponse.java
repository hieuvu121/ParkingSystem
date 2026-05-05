package ParkingSystem.demo.dto.analytics;

public record UtilizationResponse(Long zoneId, long totalSpots, long totalBookings, double utilizationPercent) {}
