package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.zone.ZoneRequest;
import ParkingSystem.demo.dto.zone.ZoneResponse;
import ParkingSystem.demo.security.JwtService;
import ParkingSystem.demo.security.UserDetailsServiceImpl;
import ParkingSystem.demo.service.ParkingZoneService;
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

@WebMvcTest(controllers = ParkingZoneController.class)
@AutoConfigureMockMvc(addFilters = false)
class ParkingZoneControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean ParkingZoneService zoneService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private ZoneResponse response() {
        return new ZoneResponse(1L, 1L, "INDOOR");
    }

    @Test
    void list_returnsAllZones() throws Exception {
        when(zoneService.listAll()).thenReturn(List.of(response()));
        mockMvc.perform(get("/api/zones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("INDOOR"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(zoneService.create(anyLong(), anyString())).thenReturn(response());
        ZoneRequest req = new ZoneRequest();
        req.setLevel(1L); req.setType("INDOOR");
        mockMvc.perform(post("/api/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_missingType_returns400() throws Exception {
        ZoneRequest req = new ZoneRequest();
        req.setLevel(1L);
        mockMvc.perform(post("/api/zones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_existingZone_returns200() throws Exception {
        when(zoneService.update(eq(1L), anyLong(), anyString())).thenReturn(response());
        ZoneRequest req = new ZoneRequest();
        req.setLevel(1L); req.setType("INDOOR");
        mockMvc.perform(put("/api/zones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("INDOOR"));
    }

    @Test
    void delete_existingZone_returns204() throws Exception {
        doNothing().when(zoneService).delete(1L);
        mockMvc.perform(delete("/api/zones/1"))
                .andExpect(status().isNoContent());
    }
}
