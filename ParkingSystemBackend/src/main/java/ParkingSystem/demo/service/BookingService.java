package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.CursorMeta;
import ParkingSystem.demo.dto.CursorPageResponse;
import ParkingSystem.demo.dto.PageResponse;
import ParkingSystem.demo.dto.booking.BookingResponse;
import ParkingSystem.demo.entity.BookingsEntity;
import ParkingSystem.demo.entity.ParkingSpotsEntity;
import ParkingSystem.demo.entity.ParkingZonesEntity;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.enums.BookingStatus;
import ParkingSystem.demo.enums.SpotStatus;
import ParkingSystem.demo.exception.ConflictException;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ParkingSpotService spotService;

    @Transactional
    public BookingResponse create(UserEntity user, Long spotId,
                                  LocalDateTime startTime, LocalDateTime endTime,
                                  String paymentType) {
        if ("SUBSCRIPTION".equals(paymentType)) {
            long active = bookingRepository.countByUserId_IdAndStatus(user.getId(), BookingStatus.APPROVED);
            if (active > 0) {
                throw new ConflictException("Subscription users can only have 1 active booking at a time");
            }
        }
        ParkingSpotsEntity spot = spotService.findOrThrow(spotId);
        List<BookingsEntity> overlapping = bookingRepository.findOverlapping(spotId, startTime, endTime);
        if (!overlapping.isEmpty()) {
            throw new ConflictException("Spot " + spotId + " is already booked for this time window");
        }
        long durationMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
        long costCents = "SUBSCRIPTION".equals(paymentType) ? 0L : (durationMinutes * 100L / 60L);

        BookingsEntity booking = BookingsEntity.builder()
                .startTime(startTime).endTime(endTime)
                .status(BookingStatus.APPROVED)
                .userId(user).spotId(spot)
                .paymentType(paymentType)
                .costCents(costCents)
                .build();
        BookingsEntity saved = bookingRepository.save(booking);
        if (!startTime.isAfter(LocalDateTime.now())) {
            spotService.updateStatus(spotId, SpotStatus.OCCUPIED);
        }
        return toResponse(saved);
    }

    public List<BookingResponse> listForUser(Long userId) {
        return bookingRepository.findByUserId_Id(userId).stream()
                .map(this::checkAndExpire).toList();
    }

    public BookingResponse getById(Long bookingId, Long userId) {
        BookingsEntity booking = findOrThrow(bookingId);
        if (!booking.getUserId().getId().equals(userId)) {
            throw new ResourceNotFoundException("Booking not found with id: " + bookingId);
        }
        return checkAndExpire(booking);
    }

    @Transactional
    public void cancel(Long bookingId, Long userId) {
        BookingsEntity booking = findOrThrow(bookingId);
        if (!booking.getUserId().getId().equals(userId)) {
            throw new ResourceNotFoundException("Booking not found with id: " + bookingId);
        }
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        spotService.updateStatus(booking.getSpotId().getId(), SpotStatus.AVAILABLE);
    }

    public PageResponse<BookingResponse> listAll(Pageable pageable) {
        return PageResponse.from(bookingRepository.findAll(pageable).map(this::toResponse));
    }

    @Transactional
    public void expireOverdue() {
        LocalDateTime now = LocalDateTime.now();
        List<Long> spotIds = bookingRepository.findExpired(now).stream()
                .map(b -> b.getSpotId().getId())
                .collect(Collectors.toList());
        if (spotIds.isEmpty()) return;
        bookingRepository.bulkExpire(now);
        spotIds.forEach(id -> spotService.updateStatus(id, SpotStatus.AVAILABLE));
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
        ParkingSpotsEntity spot = b.getSpotId();
        ParkingZonesEntity zone = spot.getZone_id();
        return new BookingResponse(
                b.getId(), spot.getId(), b.getUserId().getId(),
                b.getStartTime(), b.getEndTime(), b.getStatus(),
                spot.getRow(), spot.getCol(), spot.getType(),
                zone.getId(), zone.getLevel(), zone.getType(),
                b.getPaymentType(), b.getCostCents()
        );
    }

    public CursorPageResponse<BookingResponse> listForUserCursor(Long userId,
                                                                 String statusParam,
                                                                 String cursor,
                                                                 int limit,
                                                                 String sort) {
        int pageSize = Math.min(Math.max(limit, 1), 50);
        String normalizedSort = (sort == null) ? "desc" : sort.toLowerCase();
        boolean asc = "asc".equals(normalizedSort);
        if (!asc && !"desc".equals(normalizedSort)) {
            throw new IllegalArgumentException("Invalid sort, use 'asc' or 'desc'");
        }

        StatusFilter filter = parseStatusFilter(statusParam);
        CursorPosition cursorPos = parseCursor(cursor);
        boolean cursorEnabled = cursorPos.time != null && cursorPos.id != null;
        LocalDateTime safeTime = cursorEnabled ? cursorPos.time : LocalDateTime.of(1970, 1, 1, 0, 0);
        Long safeId = cursorEnabled ? cursorPos.id : 0L;
        PageRequest pageable = PageRequest.of(0, pageSize + 1);

        List<BookingsEntity> results = asc
                ? bookingRepository.findUserBookingsAsc(userId, filter.statuses, filter.enabled,
                cursorEnabled, safeTime, safeId, pageable)
                : bookingRepository.findUserBookingsDesc(userId, filter.statuses, filter.enabled,
                cursorEnabled, safeTime, safeId, pageable);

        boolean hasNext = results.size() > pageSize;
        List<BookingsEntity> page = hasNext ? results.subList(0, pageSize) : results;
        List<BookingResponse> content = page.stream().map(this::checkAndExpire).toList();

        String nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            BookingsEntity last = page.get(page.size() - 1);
            nextCursor = encodeCursor(last.getStartTime(), last.getId());
        }
        return new CursorPageResponse<>(content, new CursorMeta(hasNext, nextCursor));
    }

    private StatusFilter parseStatusFilter(String statusParam) {
        if (statusParam == null || statusParam.isBlank()) {
            return new StatusFilter(false, Collections.emptyList());
        }
        String normalized = statusParam.trim().toUpperCase();
        if ("ACTIVE".equals(normalized)) {
            return new StatusFilter(true, List.of(BookingStatus.PENDING, BookingStatus.APPROVED));
        }
        try {
            return new StatusFilter(true, List.of(BookingStatus.valueOf(normalized)));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid status filter");
        }
    }

    private CursorPosition parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new CursorPosition(null, null);
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", 2);
            LocalDateTime time = LocalDateTime.parse(parts[0]);
            Long id = Long.parseLong(parts[1]);
            return new CursorPosition(time, id);
        } catch (IllegalArgumentException | DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid cursor");
        }
    }

    private String encodeCursor(LocalDateTime time, Long id) {
        String raw = time + "|" + id;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private record StatusFilter(boolean enabled, List<BookingStatus> statuses) {}

    private record CursorPosition(LocalDateTime time, Long id) {}
}
