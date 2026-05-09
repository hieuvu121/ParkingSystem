package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.prediction.AvailabilityPredictionResponse;
import ParkingSystem.demo.security.JwtService;
import ParkingSystem.demo.security.UserDetailsServiceImpl;
import ParkingSystem.demo.service.PredictionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PredictionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PredictionControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean PredictionService predictionService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    @Test
    void predict_returnsAvailabilityPrediction() throws Exception {
        when(predictionService.predict(eq(1L), any(LocalDateTime.class)))
                .thenReturn(new AvailabilityPredictionResponse(1L, "2026-05-04T09:00", 0.75));

        mockMvc.perform(get("/api/predict/availability")
                        .param("zoneId", "1")
                        .param("targetTime", "2026-05-04T09:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneId").value(1))
                .andExpect(jsonPath("$.availabilityProbability").value(0.75));
    }

    @Test
    void predict_missingParams_returns400() throws Exception {
        mockMvc.perform(get("/api/predict/availability"))
                .andExpect(status().isBadRequest());
    }
}
