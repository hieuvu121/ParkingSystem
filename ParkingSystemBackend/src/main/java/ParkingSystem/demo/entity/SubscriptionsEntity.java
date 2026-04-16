package ParkingSystem.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.catalina.User;

import java.util.Date;

@Entity
@Table(name="subscriptions")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SubscriptionsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Date startDate;
    @Column(nullable = false)
    private Date endDate;

    @ManyToOne
    @JoinColumn(
            name="packageName",
            referencedColumnName="name",
            nullable = false
    )
    private PackagesEntity packageName;

    @Column(nullable = false)
    private Long price;

    @ManyToOne
    @JoinColumn(
            name="userId",
            referencedColumnName="id",
            nullable = false
    )
    private UserEntity userId;

}
