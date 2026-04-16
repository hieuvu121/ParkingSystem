package ParkingSystem.demo.entity;

import ParkingSystem.demo.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="package")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class PackagesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String description;
    @Column(nullable = false)
    private Long durations;
    @Column(nullable = false)
    private Long price;

    @OneToMany(mappedBy = "packageName")
    private List<SubscriptionsEntity> subscription=new ArrayList<>();

}
