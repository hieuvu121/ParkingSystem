package ParkingSystem.demo.dto.ws;

import ParkingSystem.demo.enums.SpotStatus;

public record SpotUpdateMessage(Long spotId, Long row, Long col, Long zoneId, SpotStatus status) {}
