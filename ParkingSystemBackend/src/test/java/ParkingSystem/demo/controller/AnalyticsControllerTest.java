package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.analytics.OccupancyResponse;
import ParkingSystem.demo.dto.analytics.PeakHourResponse;
import ParkingSystem.demo.dto.analytics.UtilizationResponse;
import ParkingSystem.demo.security.JwtService;
import ParkingSystem.demo.security.UserDetailsServiceImpl;
import ParkingSystem.demo.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AnalyticsService analyticsService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    @Test
    void occupancy_returnsOccupancyData() throws Exception {
        when(analyticsService.getOccupancy(any(), any()))
                .thenReturn(List.of(new OccupancyResponse(1L, "2026-01-01T00:00", "2026-01-02T00:00", 75.0)));
        mockMvc.perform(get("/api/analytics/occupancy")
                        .param("from", "2026-01-01T00:00:00")
                        .param("to", "2026-01-02T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].zoneId").value(1))
                .andExpect(jsonPath("$[0].occupancyPercent").value(75.0));
    }

    @Test
    void peakHours_returnsPeakHourData() throws Exception {
        when(analyticsService.getPeakHours())
                .thenReturn(List.of(new PeakHourResponse(9, 100.0)));
        mockMvc.perform(get("/api/analytics/peak-hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hour").value(9))
                .andExpect(jsonPath("$[0].averageOccupancyPercent").value(100.0));
    }

    @Test
    void utilization_returnsUtilizationData() throws Exception {
        when(analyticsService.getUtilization(any(), any()))
                .thenReturn(List.of(new UtilizationResponse(1L, 10L, 5L, 50.0)));
        mockMvc.perform(get("/api/analytics/utilization"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].utilizationPercent").value(50.0));
    }
}
