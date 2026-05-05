package ParkingSystem.demo.dto.analytics;

public record OccupancyResponse(Long zoneId, String from, String to, double occupancyPercent) {}
