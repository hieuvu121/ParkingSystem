package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.CursorMeta;
import ParkingSystem.demo.dto.CursorPageResponse;
import ParkingSystem.demo.dto.PageResponse;
import ParkingSystem.demo.dto.booking.BookingResponse;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.enums.BookingStatus;
import ParkingSystem.demo.security.JwtService;
import ParkingSystem.demo.security.UserDetailsServiceImpl;
import ParkingSystem.demo.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BookingController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookingControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean BookingService bookingService;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private BookingResponse bookingResponse() {
        return new BookingResponse(1L, 1L, 1L,
                LocalDateTime.now(), LocalDateTime.now().plusHours(2), BookingStatus.APPROVED,
                2L, 3L, "STANDARD",
                10L, 1L, "INDOOR",
                "PAY_PER_USE", 100L);
    }

    private void setAuthUser() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail("user@test.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        setAuthUser();
        when(bookingService.create(any(), anyLong(), any(), any(), any())).thenReturn(bookingResponse());
        String json = "{\"spotId\":1,\"startTime\":\"2027-01-01T10:00:00\",\"endTime\":\"2027-01-01T12:00:00\",\"paymentType\":\"PAY_PER_USE\"}";
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void create_missingSpotId_returns400() throws Exception {
        String json = "{\"startTime\":\"2027-01-01T10:00:00\",\"endTime\":\"2027-01-01T12:00:00\"}";
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void myBookings_returnsUserBookings() throws Exception {
        setAuthUser();
        CursorPageResponse<BookingResponse> page = new CursorPageResponse<>(
                List.of(bookingResponse()), new CursorMeta(false, null));
        when(bookingService.listForUserCursor(eq(1L), any(), any(), anyInt(), any()))
                .thenReturn(page);
        mockMvc.perform(get("/api/bookings/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void getBooking_returnsBooking() throws Exception {
        setAuthUser();
        when(bookingService.getById(1L, 1L)).thenReturn(bookingResponse());
        mockMvc.perform(get("/api/bookings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spotId").value(1));
    }

    @Test
    void cancel_validBooking_returns204() throws Exception {
        setAuthUser();
        doNothing().when(bookingService).cancel(1L, 1L);
        mockMvc.perform(patch("/api/bookings/1/cancel"))
                .andExpect(status().isNoContent());
    }

    @Test
    void listAll_admin_returnsAll() throws Exception {
        PageResponse<BookingResponse> page = PageResponse.from(new PageImpl<>(List.of(bookingResponse())));
        when(bookingService.listAll(any(), any(), any(), any(Pageable.class))).thenReturn(page);
        mockMvc.perform(get("/api/admin/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value(1));
    }
}
