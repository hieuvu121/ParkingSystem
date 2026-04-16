package ParkingSystem.demo.entity;

import ParkingSystem.demo.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "bookings")
@Builder
public class BookingsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private LocalDateTime startTime;

    @Column(nullable=false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private BookingStatus status;

    @ManyToOne
    @JoinColumn(
            name="createdBy",
            referencedColumnName = "id",
            nullable = false
    )
    private UserEntity userId;

    @ManyToOne
    @JoinColumn(
            name="spotId",
            referencedColumnName = "id",
            nullable = false
    )
    private ParkingSpotsEntity spotId;

}
