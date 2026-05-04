package ParkingSystem.demo.dto.subscription;

import java.util.Date;

public record SubscriptionResponse(Long id, Long userId, Long packageId,
                                    String packageName, Date startDate, Date endDate, Long price) {}
