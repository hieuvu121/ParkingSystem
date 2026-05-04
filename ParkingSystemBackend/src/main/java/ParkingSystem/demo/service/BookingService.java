package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.booking.BookingResponse;
import ParkingSystem.demo.entity.BookingsEntity;
import ParkingSystem.demo.entity.ParkingSpotsEntity;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.enums.BookingStatus;
import ParkingSystem.demo.enums.SpotStatus;
import ParkingSystem.demo.exception.ConflictException;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ParkingSpotService spotService;

    public BookingResponse create(UserEntity user, Long spotId, LocalDateTime startTime, LocalDateTime endTime) {
        ParkingSpotsEntity spot = spotService.findOrThrow(spotId);
        List<BookingsEntity> overlapping = bookingRepository.findOverlapping(spotId, startTime, endTime);
        if (!overlapping.isEmpty()) {
            throw new ConflictException("Spot " + spotId + " is already booked for this time window");
        }
        BookingsEntity booking = BookingsEntity.builder()
                .startTime(startTime).endTime(endTime)
                .status(BookingStatus.APPROVED)
                .userId(user).spotId(spot).build();
        BookingsEntity saved = bookingRepository.save(booking);
        spotService.updateStatus(spotId, SpotStatus.OCCUPIED);
        return toResponse(saved);
    }

    public List<BookingResponse> listForUser(Long userId) {
        return bookingRepository.findByUserId_Id(userId).stream()
                .map(this::checkAndExpire).toList();
    }

    public BookingResponse getById(Long bookingId, Long userId) {
        return checkAndExpire(findOrThrow(bookingId));
    }

    public void cancel(Long bookingId, Long userId) {
        BookingsEntity booking = findOrThrow(bookingId);
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        spotService.updateStatus(booking.getSpotId().getId(), SpotStatus.AVAILABLE);
    }

    public List<BookingResponse> listAll() {
        return bookingRepository.findAll().stream().map(this::toResponse).toList();
    }

    public void expireOverdue() {
        bookingRepository.findExpired(LocalDateTime.now()).forEach(b -> {
            b.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(b);
            spotService.updateStatus(b.getSpotId().getId(), SpotStatus.AVAILABLE);
        });
    }

    private BookingResponse checkAndExpire(BookingsEntity b) {
        if (b.getStatus() == BookingStatus.APPROVED && b.getEndTime().isBefore(LocalDateTime.now())) {
            b.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(b);
            spotService.updateStatus(b.getSpotId().getId(), SpotStatus.AVAILABLE);
        }
        return toResponse(b);
    }

    private BookingsEntity findOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    private BookingResponse toResponse(BookingsEntity b) {
        return new BookingResponse(b.getId(), b.getSpotId().getId(), b.getUserId().getId(),
                b.getStartTime(), b.getEndTime(), b.getStatus());
    }
}
