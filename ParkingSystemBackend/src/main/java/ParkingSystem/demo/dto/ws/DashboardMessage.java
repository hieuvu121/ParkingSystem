package ParkingSystem.demo.dto.ws;

import ParkingSystem.demo.dto.spot.ZoneSummary;

import java.util.List;

public record DashboardMessage(long total, long available, long occupied, List<ZoneSummary> byZone) {}
