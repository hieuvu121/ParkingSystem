package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.spot.ZoneSummary;
import ParkingSystem.demo.dto.ws.DashboardMessage;
import ParkingSystem.demo.dto.ws.SpotUpdateMessage;
import ParkingSystem.demo.entity.ParkingSpotsEntity;
import ParkingSystem.demo.enums.SpotStatus;
import ParkingSystem.demo.repository.ParkingSpotRepository;
import ParkingSystem.demo.repository.ParkingZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RealtimeService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ParkingSpotRepository spotRepository;
    private final ParkingZoneRepository zoneRepository;

    public void broadcastSpotUpdate(ParkingSpotsEntity spot) {
        SpotUpdateMessage msg = new SpotUpdateMessage(
                spot.getId(), spot.getRow(), spot.getCol(),
                spot.getZone_id().getId(), spot.getStatus());
        messagingTemplate.convertAndSend("/topic/spots", msg);
    }

    public void broadcastDashboard() {
        long total = spotRepository.count();
        long available = spotRepository.countByStatus(SpotStatus.AVAILABLE);
        long occupied = spotRepository.countByStatus(SpotStatus.OCCUPIED);
        List<ZoneSummary> byZone = zoneRepository.findAll().stream().map(z -> {
            long zTotal = spotRepository.countByZone_idId(z.getId());
            long zAvail = spotRepository.countByZone_idIdAndStatus(z.getId(), SpotStatus.AVAILABLE);
            return new ZoneSummary(z.getId(), zTotal, zAvail, zTotal - zAvail);
        }).toList();
        messagingTemplate.convertAndSend("/topic/dashboard",
                new DashboardMessage(total, available, occupied, byZone));
    }
}
