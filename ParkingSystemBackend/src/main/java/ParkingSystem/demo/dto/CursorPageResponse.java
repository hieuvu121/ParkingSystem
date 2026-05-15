package ParkingSystem.demo.dto;

import java.util.List;

public record CursorPageResponse<T>(
        List<T> data,
        CursorMeta meta
) {}

