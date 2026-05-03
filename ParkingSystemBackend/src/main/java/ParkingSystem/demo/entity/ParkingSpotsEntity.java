package ParkingSystem.demo.entity;

import ParkingSystem.demo.enums.SpotStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "parkingSpots")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ParkingSpotsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long row;

    @Column(nullable = false)
    private Long col;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SpotStatus status;

    @OneToMany(mappedBy = "spotId")
    private List<BookingsEntity> bookings;

    @ManyToOne
    @JoinColumn(name = "zone_id", referencedColumnName = "id", nullable = false)
    private ParkingZonesEntity zone_id;
}
