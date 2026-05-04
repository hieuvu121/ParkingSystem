package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.booking.BookingRequest;
import ParkingSystem.demo.dto.booking.BookingResponse;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/bookings")
    @PreAuthorize("hasRole('USERS')")
    public ResponseEntity<BookingResponse> create(@AuthenticationPrincipal UserEntity user,
                                                  @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                bookingService.create(user, request.getSpotId(), request.getStartTime(), request.getEndTime()));
    }

    @GetMapping("/bookings/my")
    @PreAuthorize("hasRole('USERS')")
    public ResponseEntity<List<BookingResponse>> myBookings(@AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(bookingService.listForUser(user.getId()));
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable Long id,
                                                      @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(bookingService.getById(id, user.getId()));
    }

    @PatchMapping("/bookings/{id}/cancel")
    @PreAuthorize("hasRole('USERS')")
    public ResponseEntity<Void> cancel(@PathVariable Long id,
                                       @AuthenticationPrincipal UserEntity user) {
        bookingService.cancel(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookingResponse>> listAll() {
        return ResponseEntity.ok(bookingService.listAll());
    }
}
