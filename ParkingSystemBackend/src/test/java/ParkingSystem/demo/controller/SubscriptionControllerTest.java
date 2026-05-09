package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.subscription.SubscriptionRequest;
import ParkingSystem.demo.dto.subscription.SubscriptionResponse;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.security.JwtService;
import ParkingSystem.demo.security.UserDetailsServiceImpl;
import ParkingSystem.demo.service.SubscriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SubscriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SubscriptionControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean SubscriptionService subscriptionService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private SubscriptionResponse response() {
        return new SubscriptionResponse(1L, 1L, 1L, "Monthly", new Date(), new Date(), 500000L);
    }

    private void setAuthUser() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail("user@test.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @Test
    void subscribe_validRequest_returns201() throws Exception {
        setAuthUser();
        when(subscriptionService.subscribe(any(UserEntity.class), eq(1L))).thenReturn(response());
        SubscriptionRequest req = new SubscriptionRequest();
        req.setPackageId(1L);
        mockMvc.perform(post("/api/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.packageName").value("Monthly"));
    }

    @Test
    void subscribe_missingPackageId_returns400() throws Exception {
        SubscriptionRequest req = new SubscriptionRequest();
        mockMvc.perform(post("/api/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mySubscription_returnsActiveSubscription() throws Exception {
        setAuthUser();
        when(subscriptionService.getActiveSubscription(1L)).thenReturn(response());
        mockMvc.perform(get("/api/subscriptions/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(500000));
    }

    @Test
    void listAll_admin_returnsAllSubscriptions() throws Exception {
        when(subscriptionService.listAll()).thenReturn(List.of(response()));
        mockMvc.perform(get("/api/admin/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].packageName").value("Monthly"));
    }
}
