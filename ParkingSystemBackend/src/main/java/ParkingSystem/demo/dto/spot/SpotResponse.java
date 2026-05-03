package ParkingSystem.demo.dto.spot;

import ParkingSystem.demo.enums.SpotStatus;

public record SpotResponse(Long id, Long row, Long col, String type, SpotStatus status, Long zoneId) {}
