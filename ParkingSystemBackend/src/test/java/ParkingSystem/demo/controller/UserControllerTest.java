package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.user.UpdateProfileRequest;
import ParkingSystem.demo.dto.user.UpdateRoleRequest;
import ParkingSystem.demo.dto.user.UserProfileResponse;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.enums.Role;
import ParkingSystem.demo.security.JwtService;
import ParkingSystem.demo.security.UserDetailsServiceImpl;
import ParkingSystem.demo.service.UserService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean UserService userService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private UserProfileResponse profile(Long id) {
        return new UserProfileResponse(id, "John Doe", "john@test.com", Role.USERS);
    }

    private void setAuthUser(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail("john@test.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @Test
    void getMe_returnsOwnProfile() throws Exception {
        setAuthUser(1L);
        when(userService.getProfile(1L)).thenReturn(profile(1L));
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@test.com"));
    }

    @Test
    void updateMe_validRequest_returns200() throws Exception {
        setAuthUser(1L);
        when(userService.updateProfile(eq(1L), anyString())).thenReturn(profile(1L));
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("John Doe");
        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("John Doe"));
    }

    @Test
    void updateMe_blankName_returns400() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("");
        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listUsers_admin_returnsAll() throws Exception {
        when(userService.listAll()).thenReturn(List.of(profile(1L), profile(2L)));
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void deleteUser_admin_returns204() throws Exception {
        doNothing().when(userService).deleteUser(1L);
        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isNoContent());
    }
}
