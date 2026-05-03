package ParkingSystem.demo.dto.spot;

import java.util.List;

public record DashboardResponse(long total, long available, long occupied, List<ZoneSummary> byZone) {}
