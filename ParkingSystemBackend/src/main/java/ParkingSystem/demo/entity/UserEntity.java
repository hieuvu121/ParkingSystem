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
@Table(name="users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;
    @Column(nullable = false,unique = true)
    private String email;
    @Column(nullable = false)
    private Role role;
    @Column(nullable = false)
    private String password;

    @OneToMany(mappedBy = "userId")
    private List<SubscriptionsEntity> subscriptionName=new ArrayList<>();

    @OneToMany(mappedBy = "userId")
    private List<BookingsEntity> bookings=new ArrayList<>();


}
