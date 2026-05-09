package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.spot.*;
import ParkingSystem.demo.enums.SpotStatus;
import ParkingSystem.demo.security.JwtService;
import ParkingSystem.demo.security.UserDetailsServiceImpl;
import ParkingSystem.demo.service.ParkingSpotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ParkingSpotController.class)
@AutoConfigureMockMvc(addFilters = false)
class ParkingSpotControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean ParkingSpotService spotService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private SpotResponse spotResponse() {
        return new SpotResponse(1L, 1L, 2L, "STANDARD", SpotStatus.AVAILABLE, 1L);
    }

    @Test
    void listByZone_returnsSpots() throws Exception {
        when(spotService.listByZone(1L)).thenReturn(List.of(spotResponse()));
        mockMvc.perform(get("/api/zones/1/spots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("STANDARD"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(spotService.create(eq(1L), anyLong(), anyLong(), anyString())).thenReturn(spotResponse());
        SpotRequest req = new SpotRequest();
        req.setRow(1L); req.setCol(2L); req.setType("STANDARD");
        mockMvc.perform(post("/api/zones/1/spots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_missingType_returns400() throws Exception {
        SpotRequest req = new SpotRequest();
        req.setRow(1L); req.setCol(2L);
        mockMvc.perform(post("/api/zones/1/spots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_existingSpot_returns200() throws Exception {
        when(spotService.update(eq(1L), anyLong(), anyLong(), anyString())).thenReturn(spotResponse());
        SpotRequest req = new SpotRequest();
        req.setRow(1L); req.setCol(2L); req.setType("STANDARD");
        mockMvc.perform(put("/api/spots/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_existingSpot_returns204() throws Exception {
        doNothing().when(spotService).delete(1L);
        mockMvc.perform(delete("/api/spots/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void dashboard_returnsSummary() throws Exception {
        when(spotService.getDashboard()).thenReturn(new DashboardResponse(10, 6, 4, List.of()));
        mockMvc.perform(get("/api/spots/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10));
    }

    @Test
    void webhook_validRequest_returns200() throws Exception {
        doNothing().when(spotService).updateStatus(any(), any());
        SpotStatusUpdateRequest req = new SpotStatusUpdateRequest();
        req.setSpotId(1L); req.setStatus(SpotStatus.OCCUPIED);
        mockMvc.perform(post("/api/spots/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void simulate_returns200() throws Exception {
        doNothing().when(spotService).simulate(any());
        mockMvc.perform(post("/api/spots/simulate"))
                .andExpect(status().isOk());
    }
}
