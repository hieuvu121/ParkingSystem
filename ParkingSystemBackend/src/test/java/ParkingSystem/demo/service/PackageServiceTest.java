package ParkingSystem.demo.service;

import ParkingSystem.demo.entity.PackagesEntity;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.PackageRepository;
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
class PackageServiceTest {

    @Mock private PackageRepository packageRepository;
    @InjectMocks private PackageService packageService;

    private PackagesEntity pkg(Long id) {
        return PackagesEntity.builder().id(id).name("Monthly").description("desc")
                .durations(30L).price(500000L).build();
    }

    @Test
    void listAll_returnsAllPackages() {
        when(packageRepository.findAll()).thenReturn(List.of(pkg(1L), pkg(2L)));
        assertThat(packageService.listAll()).hasSize(2);
    }

    @Test
    void create_savesAndReturnsPackage() {
        when(packageRepository.save(any())).thenReturn(pkg(1L));
        var result = packageService.create("Monthly", "desc", 30L, 500000L);
        assertThat(result.name()).isEqualTo("Monthly");
        assertThat(result.durations()).isEqualTo(30L);
    }

    @Test
    void update_existingPackage_updatesFields() {
        var entity = pkg(1L);
        when(packageRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(packageRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = packageService.update(1L, "Weekly", "new desc", 7L, 100000L);
        assertThat(result.name()).isEqualTo("Weekly");
        assertThat(result.durations()).isEqualTo(7L);
        assertThat(result.price()).isEqualTo(100000L);
    }

    @Test
    void update_unknownPackage_throwsNotFound() {
        when(packageRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> packageService.update(99L, "x", "x", 1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_existingPackage_callsDelete() {
        when(packageRepository.existsById(1L)).thenReturn(true);
        packageService.delete(1L);
        verify(packageRepository).deleteById(1L);
    }

    @Test
    void delete_unknownPackage_throwsNotFound() {
        when(packageRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> packageService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findOrThrow_existingPackage_returnsEntity() {
        when(packageRepository.findById(1L)).thenReturn(Optional.of(pkg(1L)));
        assertThat(packageService.findOrThrow(1L).getId()).isEqualTo(1L);
    }

    @Test
    void findOrThrow_unknownPackage_throwsNotFound() {
        when(packageRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> packageService.findOrThrow(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
