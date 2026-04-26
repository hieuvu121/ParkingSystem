package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.AuthResponse;
import ParkingSystem.demo.dto.LoginRequest;
import ParkingSystem.demo.dto.RegisterRequest;
import ParkingSystem.demo.security.JwtService;
import ParkingSystem.demo.security.UserDetailsServiceImpl;
import ParkingSystem.demo.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void register_returns201WithMessage() throws Exception {
        RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "password123");
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(Map.of("message", "Registration successful. Please check your email to verify your account."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful. Please check your email to verify your account."));
    }

    @Test
    void register_withMissingEmail_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("John", "", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verify_withValidToken_returns200WithMessage() throws Exception {
        when(authService.verify("valid-uuid"))
                .thenReturn(Map.of("message", "Account verified successfully. You can now log in."));

        mockMvc.perform(get("/api/auth/verify").param("token", "valid-uuid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account verified successfully. You can now log in."));
    }

    @Test
    void login_withValidCredentials_returns200WithToken() throws Exception {
        LoginRequest request = new LoginRequest("john@example.com", "password123");
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("mock.jwt.token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock.jwt.token"));
    }
}
