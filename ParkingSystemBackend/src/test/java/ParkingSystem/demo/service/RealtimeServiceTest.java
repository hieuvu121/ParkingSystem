package ParkingSystem.demo.service;

import ParkingSystem.demo.entity.ParkingSpotsEntity;
import ParkingSystem.demo.entity.ParkingZonesEntity;
import ParkingSystem.demo.enums.SpotStatus;
import ParkingSystem.demo.repository.ParkingSpotRepository;
import ParkingSystem.demo.repository.ParkingZoneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RealtimeServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ParkingSpotRepository spotRepository;
    @Mock private ParkingZoneRepository zoneRepository;
    @InjectMocks private RealtimeService realtimeService;

    private ParkingSpotsEntity spot() {
        ParkingZonesEntity zone = ParkingZonesEntity.builder().id(1L).level(1L).type("INDOOR").build();
        return ParkingSpotsEntity.builder()
                .id(1L).row(1L).col(2L).type("STANDARD")
                .status(SpotStatus.AVAILABLE).zone_id(zone).build();
    }

    @Test
    void broadcastSpotUpdate_sendsToSpotsTopic() {
        realtimeService.broadcastSpotUpdate(spot());
        verify(messagingTemplate).convertAndSend(eq("/topic/spots"), any(Object.class));
    }

    @Test
    void broadcastDashboard_sendsTopoicDashboard() {
        when(spotRepository.count()).thenReturn(10L);
        when(spotRepository.countByStatus(SpotStatus.AVAILABLE)).thenReturn(6L);
        when(spotRepository.countByStatus(SpotStatus.OCCUPIED)).thenReturn(4L);
        when(zoneRepository.findAll()).thenReturn(List.of());

        realtimeService.broadcastDashboard();

        verify(messagingTemplate).convertAndSend(eq("/topic/dashboard"), any(Object.class));
    }

    @Test
    void broadcastDashboard_includesZoneSummaries() {
        ParkingZonesEntity zone = ParkingZonesEntity.builder().id(1L).level(1L).type("INDOOR").build();
        when(spotRepository.count()).thenReturn(2L);
        when(spotRepository.countByStatus(SpotStatus.AVAILABLE)).thenReturn(1L);
        when(spotRepository.countByStatus(SpotStatus.OCCUPIED)).thenReturn(1L);
        when(zoneRepository.findAll()).thenReturn(List.of(zone));
        when(spotRepository.countByZone_idId(1L)).thenReturn(2L);
        when(spotRepository.countByZone_idIdAndStatus(1L, SpotStatus.AVAILABLE)).thenReturn(1L);

        realtimeService.broadcastDashboard();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/dashboard"), captor.capture());
        assertThat(captor.getValue().toString()).contains("byZone");
    }
}
