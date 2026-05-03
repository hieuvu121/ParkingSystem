package ParkingSystem.demo.service;

import ParkingSystem.demo.entity.ParkingZonesEntity;
import ParkingSystem.demo.exception.ResourceNotFoundException;
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
class ParkingZoneServiceTest {

    @Mock private ParkingZoneRepository zoneRepository;
    @InjectMocks private ParkingZoneService zoneService;

    private ParkingZonesEntity zone(Long id) {
        return ParkingZonesEntity.builder().id(id).level(1L).type("STANDARD").build();
    }

    @Test
    void listAll_returnsAllZones() {
        when(zoneRepository.findAll()).thenReturn(List.of(zone(1L), zone(2L)));
        assertThat(zoneService.listAll()).hasSize(2);
    }

    @Test
    void create_savesAndReturnsZone() {
        var entity = zone(1L);
        when(zoneRepository.save(any())).thenReturn(entity);
        var result = zoneService.create(1L, "STANDARD");
        assertThat(result.id()).isEqualTo(1L);
    }

    @Test
    void update_existingZone_updatesFields() {
        var entity = zone(1L);
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(zoneRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = zoneService.update(1L, 2L, "VIP");
        assertThat(result.level()).isEqualTo(2L);
        assertThat(result.type()).isEqualTo("VIP");
    }

    @Test
    void delete_existingId_deletesZone() {
        when(zoneRepository.existsById(1L)).thenReturn(true);
        zoneService.delete(1L);
        verify(zoneRepository).deleteById(1L);
    }

    @Test
    void delete_unknownId_throwsNotFound() {
        when(zoneRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> zoneService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findOrThrow_unknownId_throwsNotFound() {
        when(zoneRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> zoneService.findOrThrow(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
