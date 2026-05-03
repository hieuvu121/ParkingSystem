package ParkingSystem.demo.service;

import ParkingSystem.demo.entity.ParkingZonesEntity;
import ParkingSystem.demo.entity.ParkingSpotsEntity;
import ParkingSystem.demo.enums.SpotStatus;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.ParkingSpotRepository;
import ParkingSystem.demo.repository.ParkingZoneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingSpotServiceTest {

    @Mock private ParkingSpotRepository spotRepository;
    @Mock private ParkingZoneService zoneService;
    @Mock private RealtimeService realtimeService;
    @Mock private ParkingZoneRepository zoneRepository;
    @InjectMocks private ParkingSpotService spotService;

    private ParkingZonesEntity zone() {
        return ParkingZonesEntity.builder().id(1L).level(1L).type("STD").build();
    }

    private ParkingSpotsEntity spot(Long id, SpotStatus status) {
        return ParkingSpotsEntity.builder().id(id).row(1L).col(1L).type("CAR")
                .status(status).zone_id(zone()).build();
    }

    @Test
    void updateStatus_persistsAndBroadcasts() {
        var s = spot(1L, SpotStatus.AVAILABLE);
        when(spotRepository.findById(1L)).thenReturn(Optional.of(s));
        when(spotRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        spotService.updateStatus(1L, SpotStatus.OCCUPIED);
        verify(spotRepository).save(argThat(sp -> sp.getStatus() == SpotStatus.OCCUPIED));
        verify(realtimeService).broadcastSpotUpdate(any());
        verify(realtimeService).broadcastDashboard();
    }

    @Test
    void updateStatus_unknownSpot_throwsNotFound() {
        when(spotRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> spotService.updateStatus(99L, SpotStatus.OCCUPIED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listByZone_returnsSpots() {
        when(spotRepository.findByZone_idId(1L)).thenReturn(List.of(spot(1L, SpotStatus.AVAILABLE)));
        assertThat(spotService.listByZone(1L)).hasSize(1);
    }

    @Test
    void create_savesSpotWithAvailableStatus() {
        when(zoneService.findOrThrow(1L)).thenReturn(zone());
        when(spotRepository.save(any())).thenAnswer(i -> {
            var sp = (ParkingSpotsEntity) i.getArgument(0);
            return ParkingSpotsEntity.builder().id(10L).row(sp.getRow()).col(sp.getCol())
                    .type(sp.getType()).status(sp.getStatus()).zone_id(sp.getZone_id()).build();
        });
        var result = spotService.create(1L, 2L, 3L, "CAR");
        assertThat(result.status()).isEqualTo(SpotStatus.AVAILABLE);
        assertThat(result.id()).isEqualTo(10L);
    }

    @Test
    void delete_unknownId_throwsNotFound() {
        when(spotRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> spotService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
