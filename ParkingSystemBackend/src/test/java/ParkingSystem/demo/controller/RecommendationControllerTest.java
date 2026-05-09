package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.recommendation.RecommendationResponse;
import ParkingSystem.demo.security.JwtService;
import ParkingSystem.demo.security.UserDetailsServiceImpl;
import ParkingSystem.demo.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecommendationControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean RecommendationService recommendationService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    @Test
    void recommend_withZones_returns200() throws Exception {
        when(recommendationService.recommend(any(), any()))
                .thenReturn(Optional.of(new RecommendationResponse(1L, "Nearest available zone", 5, 0.8)));

        mockMvc.perform(get("/api/recommend/spot").param("lat", "10.0").param("lng", "106.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneId").value(1))
                .andExpect(jsonPath("$.reason").value("Nearest available zone"));
    }

    @Test
    void recommend_noZonesAvailable_returns404() throws Exception {
        when(recommendationService.recommend(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/recommend/spot"))
                .andExpect(status().isNotFound());
    }

    @Test
    void recommend_withoutLatLng_returns200() throws Exception {
        when(recommendationService.recommend(any(), any()))
                .thenReturn(Optional.of(new RecommendationResponse(2L, "Least congested zone", 3, 0.6)));

        mockMvc.perform(get("/api/recommend/spot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneId").value(2));
    }
}
