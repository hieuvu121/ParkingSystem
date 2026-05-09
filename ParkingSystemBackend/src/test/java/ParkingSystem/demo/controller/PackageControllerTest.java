package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.pkg.PackageRequest;
import ParkingSystem.demo.dto.pkg.PackageResponse;
import ParkingSystem.demo.security.JwtService;
import ParkingSystem.demo.security.UserDetailsServiceImpl;
import ParkingSystem.demo.service.PackageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PackageController.class)
@AutoConfigureMockMvc(addFilters = false)
class PackageControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean PackageService packageService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private PackageResponse response() {
        return new PackageResponse(1L, "Monthly", "desc", 30L, 500000L);
    }

    @Test
    void list_returnsAllPackages() throws Exception {
        when(packageService.listAll()).thenReturn(List.of(response()));
        mockMvc.perform(get("/api/packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Monthly"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        when(packageService.create(any(), any(), any(), any())).thenReturn(response());
        PackageRequest req = new PackageRequest();
        req.setName("Monthly"); req.setDescription("desc");
        req.setDurations(30L); req.setPrice(500000L);
        mockMvc.perform(post("/api/packages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_missingName_returns400() throws Exception {
        PackageRequest req = new PackageRequest();
        req.setDescription("desc"); req.setDurations(30L); req.setPrice(500000L);
        mockMvc.perform(post("/api/packages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_existingPackage_returns200() throws Exception {
        when(packageService.update(eq(1L), any(), any(), any(), any())).thenReturn(response());
        PackageRequest req = new PackageRequest();
        req.setName("Monthly"); req.setDescription("desc");
        req.setDurations(30L); req.setPrice(500000L);
        mockMvc.perform(put("/api/packages/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Monthly"));
    }

    @Test
    void delete_existingPackage_returns204() throws Exception {
        doNothing().when(packageService).delete(1L);
        mockMvc.perform(delete("/api/packages/1"))
                .andExpect(status().isNoContent());
    }
}
