package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.CursorPageResponse;
import ParkingSystem.demo.dto.PageResponse;
import ParkingSystem.demo.dto.booking.BookingRequest;
import ParkingSystem.demo.dto.booking.BookingResponse;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/bookings")
    @PreAuthorize("hasRole('USERS')")
    public ResponseEntity<BookingResponse> create(@AuthenticationPrincipal UserEntity user,
                                                  @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                bookingService.create(user, request.getSpotId(), request.getStartTime(), request.getEndTime(), request.getPaymentType()));
    }

    @GetMapping("/bookings/my")
    @PreAuthorize("hasRole('USERS')")
    public ResponseEntity<CursorPageResponse<BookingResponse>> myBookings(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "desc") String sort) {
        return ResponseEntity.ok(bookingService.listForUserCursor(user.getId(), status, cursor, limit, sort));
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
    public ResponseEntity<PageResponse<BookingResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        LocalDateTime resolvedFrom = from != null ? from : LocalDateTime.now().minusDays(30);
        LocalDateTime resolvedTo   = to   != null ? to   : LocalDateTime.now();
        return ResponseEntity.ok(bookingService.listAll(status, resolvedFrom, resolvedTo, PageRequest.of(page, size)));
    }
}
