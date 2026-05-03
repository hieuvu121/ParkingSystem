# Parking System Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Phases 1–6 of the parking system backend: user management, parking zones/spots, bookings, packages/subscriptions, real-time WebSocket, and analytics/predictions/recommendations.

**Architecture:** Domain-by-domain vertical slices in dependency order. All real-time broadcasts flow through `ParkingSpotService.updateStatus()` → `RealtimeService`. Exception handling is centralized via `GlobalExceptionHandler`.

**Tech Stack:** Spring Boot 4.0, Spring Data JPA, Spring Security (JWT + `@PreAuthorize`), Spring WebSocket (STOMP/SimpleBroker), PostgreSQL, Lombok, Mockito, AssertJ

---

All source files under: `ParkingSystemBackend/src/main/java/ParkingSystem/demo/`
All test files under: `ParkingSystemBackend/src/test/java/ParkingSystem/demo/`
Run tests from `ParkingSystemBackend/` directory: `./mvnw test -Dtest=ClassName`
Compile check: `./mvnw compile`

---

## Task 1: Foundation — Fix Entities/Enums, Add Exception Infrastructure

**Files:**
- Modify: `enums/BookingStatus.java`
- Modify: `entity/ParkingSpotsEntity.java`
- Modify: `security/SecurityConfig.java`
- Create: `exception/ErrorResponse.java`
- Create: `exception/ResourceNotFoundException.java`
- Create: `exception/ConflictException.java`
- Create: `exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Add CANCELLED to BookingStatus**

`enums/BookingStatus.java`:
```java
package ParkingSystem.demo.enums;

public enum BookingStatus {
    PENDING, APPROVED, EXPIRED, CANCELLED
}
```

- [ ] **Step 2: Fix ParkingSpotsEntity — change status type to SpotStatus**

`entity/ParkingSpotsEntity.java`:
```java
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
```

- [ ] **Step 3: Fix SecurityConfig — class declaration, add @EnableMethodSecurity, permit /ws**

`security/SecurityConfig.java`:
```java
package ParkingSystem.demo.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/verify").permitAll()
                        .requestMatchers("/error", "/ws/**").permitAll()
                        .anyRequest().authenticated()
                )
                .userDetailsService(userDetailsService)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

- [ ] **Step 4: Create ErrorResponse record**

`exception/ErrorResponse.java`:
```java
package ParkingSystem.demo.exception;

public record ErrorResponse(String error, String message) {}
```

- [ ] **Step 5: Create ResourceNotFoundException**

`exception/ResourceNotFoundException.java`:
```java
package ParkingSystem.demo.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

- [ ] **Step 6: Create ConflictException**

`exception/ConflictException.java`:
```java
package ParkingSystem.demo.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
```

- [ ] **Step 7: Create GlobalExceptionHandler**

`exception/GlobalExceptionHandler.java`:
```java
package ParkingSystem.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("Conflict", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Validation Failed", message));
    }
}
```

- [ ] **Step 8: Compile to verify no errors**

```bash
cd ParkingSystemBackend && ./mvnw compile
```
Expected: `BUILD SUCCESS`

- [ ] **Step 9: Commit**

```bash
git add ParkingSystemBackend/src/main/java/ParkingSystem/demo/enums/BookingStatus.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/entity/ParkingSpotsEntity.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/security/SecurityConfig.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/exception/
git commit -m "feat: fix entities/enums and add exception infrastructure"
```

---

## Task 2: User Management

**Files:**
- Create: `dto/user/UserProfileResponse.java`
- Create: `dto/user/UpdateProfileRequest.java`
- Create: `dto/user/UpdateRoleRequest.java`
- Create: `service/UserService.java`
- Create: `controller/UserController.java`
- Create: `test/.../service/UserServiceTest.java`

- [ ] **Step 1: Write failing UserService test**

`test/.../service/UserServiceTest.java`:
```java
package ParkingSystem.demo.service;

import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.enums.Role;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.UserRepository;
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
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private UserService userService;

    private UserEntity user(Long id, String name, Role role) {
        return UserEntity.builder().id(id).fullName(name).email("u@x.com").role(role)
                .password("pw").isActive(true).build();
    }

    @Test
    void getProfile_existingUser_returnsProfile() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "Alice", Role.USERS)));
        var result = userService.getProfile(1L);
        assertThat(result.fullName()).isEqualTo("Alice");
    }

    @Test
    void getProfile_unknownUser_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_existingUser_updatesFullName() {
        var u = user(1L, "Alice", Role.USERS);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = userService.updateProfile(1L, "Bob");
        assertThat(result.fullName()).isEqualTo("Bob");
    }

    @Test
    void changeRole_existingUser_updatesRole() {
        var u = user(1L, "Alice", Role.USERS);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = userService.changeRole(1L, Role.ADMIN);
        assertThat(result.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void deleteUser_existingUser_callsDelete() {
        when(userRepository.existsById(1L)).thenReturn(true);
        userService.deleteUser(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_unknownUser_throwsNotFound() {
        when(userRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

- [ ] **Step 2: Run test — expect FAIL (UserService not found)**

```bash
cd ParkingSystemBackend && ./mvnw test -Dtest=UserServiceTest
```
Expected: compilation error or test failure

- [ ] **Step 3: Create DTOs**

`dto/user/UserProfileResponse.java`:
```java
package ParkingSystem.demo.dto.user;

import ParkingSystem.demo.enums.Role;

public record UserProfileResponse(Long id, String fullName, String email, Role role) {}
```

`dto/user/UpdateProfileRequest.java`:
```java
package ParkingSystem.demo.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @NotBlank
    private String fullName;
}
```

`dto/user/UpdateRoleRequest.java`:
```java
package ParkingSystem.demo.dto.user;

import ParkingSystem.demo.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateRoleRequest {
    @NotNull
    private Role role;
}
```

- [ ] **Step 4: Create UserService**

`service/UserService.java`:
```java
package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.user.UserProfileResponse;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.enums.Role;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserProfileResponse getProfile(Long userId) {
        return toResponse(findOrThrow(userId));
    }

    public UserProfileResponse updateProfile(Long userId, String fullName) {
        UserEntity user = findOrThrow(userId);
        user.setFullName(fullName);
        return toResponse(userRepository.save(user));
    }

    public UserProfileResponse changeRole(Long userId, Role role) {
        UserEntity user = findOrThrow(userId);
        user.setRole(role);
        return toResponse(userRepository.save(user));
    }

    public List<UserProfileResponse> listAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
    }

    private UserEntity findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private UserProfileResponse toResponse(UserEntity u) {
        return new UserProfileResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole());
    }
}
```

- [ ] **Step 5: Run test — expect PASS**

```bash
cd ParkingSystemBackend && ./mvnw test -Dtest=UserServiceTest
```
Expected: `Tests run: 6, Failures: 0`

- [ ] **Step 6: Create UserController**

`controller/UserController.java`:
```java
package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.user.UpdateProfileRequest;
import ParkingSystem.demo.dto.user.UpdateRoleRequest;
import ParkingSystem.demo.dto.user.UserProfileResponse;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/api/users/me")
    public ResponseEntity<UserProfileResponse> getMe(@AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(userService.getProfile(user.getId()));
    }

    @PutMapping("/api/users/me")
    public ResponseEntity<UserProfileResponse> updateMe(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(user.getId(), request.getFullName()));
    }

    @GetMapping("/api/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileResponse>> listUsers() {
        return ResponseEntity.ok(userService.listAll());
    }

    @PatchMapping("/api/admin/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(userService.changeRole(id, request.getRole()));
    }

    @DeleteMapping("/api/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 7: Compile**

```bash
cd ParkingSystemBackend && ./mvnw compile
```
Expected: `BUILD SUCCESS`

- [ ] **Step 8: Commit**

```bash
git add ParkingSystemBackend/src/main/java/ParkingSystem/demo/dto/user/ \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/service/UserService.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/controller/UserController.java \
  ParkingSystemBackend/src/test/java/ParkingSystem/demo/service/UserServiceTest.java
git commit -m "feat: user management endpoints"
```

---

## Task 3: Parking Zones

**Files:**
- Create: `dto/zone/ZoneRequest.java`
- Create: `dto/zone/ZoneResponse.java`
- Create: `repository/ParkingZoneRepository.java`
- Create: `service/ParkingZoneService.java`
- Create: `controller/ParkingZoneController.java`
- Create: `test/.../service/ParkingZoneServiceTest.java`

- [ ] **Step 1: Write failing ParkingZoneService test**

`test/.../service/ParkingZoneServiceTest.java`:
```java
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
    void delete_unknownId_throwsNotFound() {
        when(zoneRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> zoneService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

```bash
cd ParkingSystemBackend && ./mvnw test -Dtest=ParkingZoneServiceTest
```

- [ ] **Step 3: Create DTOs**

`dto/zone/ZoneRequest.java`:
```java
package ParkingSystem.demo.dto.zone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ZoneRequest {
    @NotNull
    private Long level;
    @NotBlank
    private String type;
}
```

`dto/zone/ZoneResponse.java`:
```java
package ParkingSystem.demo.dto.zone;

public record ZoneResponse(Long id, Long level, String type) {}
```

- [ ] **Step 4: Create ParkingZoneRepository**

`repository/ParkingZoneRepository.java`:
```java
package ParkingSystem.demo.repository;

import ParkingSystem.demo.entity.ParkingZonesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParkingZoneRepository extends JpaRepository<ParkingZonesEntity, Long> {}
```

- [ ] **Step 5: Create ParkingZoneService**

`service/ParkingZoneService.java`:
```java
package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.zone.ZoneResponse;
import ParkingSystem.demo.entity.ParkingZonesEntity;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.ParkingZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParkingZoneService {

    private final ParkingZoneRepository zoneRepository;

    public List<ZoneResponse> listAll() {
        return zoneRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ZoneResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    public ZoneResponse create(Long level, String type) {
        ParkingZonesEntity zone = ParkingZonesEntity.builder().level(level).type(type).build();
        return toResponse(zoneRepository.save(zone));
    }

    public ZoneResponse update(Long id, Long level, String type) {
        ParkingZonesEntity zone = findOrThrow(id);
        zone.setLevel(level);
        zone.setType(type);
        return toResponse(zoneRepository.save(zone));
    }

    public void delete(Long id) {
        if (!zoneRepository.existsById(id)) {
            throw new ResourceNotFoundException("Zone not found with id: " + id);
        }
        zoneRepository.deleteById(id);
    }

    public ParkingZonesEntity findOrThrow(Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone not found with id: " + id));
    }

    private ZoneResponse toResponse(ParkingZonesEntity z) {
        return new ZoneResponse(z.getId(), z.getLevel(), z.getType());
    }
}
```

- [ ] **Step 6: Run test — expect PASS**

```bash
cd ParkingSystemBackend && ./mvnw test -Dtest=ParkingZoneServiceTest
```
Expected: `Tests run: 3, Failures: 0`

- [ ] **Step 7: Create ParkingZoneController**

`controller/ParkingZoneController.java`:
```java
package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.zone.ZoneRequest;
import ParkingSystem.demo.dto.zone.ZoneResponse;
import ParkingSystem.demo.service.ParkingZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/zones")
@RequiredArgsConstructor
public class ParkingZoneController {

    private final ParkingZoneService zoneService;

    @GetMapping
    public ResponseEntity<List<ZoneResponse>> list() {
        return ResponseEntity.ok(zoneService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ZoneResponse> create(@Valid @RequestBody ZoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(zoneService.create(request.getLevel(), request.getType()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ZoneResponse> update(@PathVariable Long id, @Valid @RequestBody ZoneRequest request) {
        return ResponseEntity.ok(zoneService.update(id, request.getLevel(), request.getType()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        zoneService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 8: Compile and commit**

```bash
cd ParkingSystemBackend && ./mvnw compile
git add ParkingSystemBackend/src/main/java/ParkingSystem/demo/dto/zone/ \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/repository/ParkingZoneRepository.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/service/ParkingZoneService.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/controller/ParkingZoneController.java \
  ParkingSystemBackend/src/test/java/ParkingSystem/demo/service/ParkingZoneServiceTest.java
git commit -m "feat: parking zone CRUD"
```

---

## Task 4: Parking Spots + RealtimeService Stub

**Files:**
- Create: `dto/spot/SpotRequest.java`
- Create: `dto/spot/SpotResponse.java`
- Create: `dto/spot/SpotStatusUpdateRequest.java`
- Create: `dto/spot/ZoneSummary.java`
- Create: `dto/spot/DashboardResponse.java`
- Create: `service/RealtimeService.java` (stub)
- Create: `repository/ParkingSpotRepository.java`
- Create: `service/ParkingSpotService.java`
- Create: `controller/ParkingSpotController.java`
- Create: `test/.../service/ParkingSpotServiceTest.java`

- [ ] **Step 1: Write failing ParkingSpotService test**

`test/.../service/ParkingSpotServiceTest.java`:
```java
package ParkingSystem.demo.service;

import ParkingSystem.demo.entity.ParkingZonesEntity;
import ParkingSystem.demo.entity.ParkingSpotsEntity;
import ParkingSystem.demo.enums.SpotStatus;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.ParkingSpotRepository;
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
    @InjectMocks private ParkingSpotService spotService;

    private ParkingZonesEntity zone() {
        return ParkingZonesEntity.builder().id(1L).level(1L).type("STANDARD").build();
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
}
```

- [ ] **Step 2: Run test — expect FAIL**

```bash
cd ParkingSystemBackend && ./mvnw test -Dtest=ParkingSpotServiceTest
```

- [ ] **Step 3: Create Spot DTOs**

`dto/spot/SpotRequest.java`:
```java
package ParkingSystem.demo.dto.spot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SpotRequest {
    @NotNull private Long row;
    @NotNull private Long col;
    @NotBlank private String type;
}
```

`dto/spot/SpotResponse.java`:
```java
package ParkingSystem.demo.dto.spot;

import ParkingSystem.demo.enums.SpotStatus;

public record SpotResponse(Long id, Long row, Long col, String type, SpotStatus status, Long zoneId) {}
```

`dto/spot/SpotStatusUpdateRequest.java`:
```java
package ParkingSystem.demo.dto.spot;

import ParkingSystem.demo.enums.SpotStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SpotStatusUpdateRequest {
    @NotNull private Long spotId;
    @NotNull private SpotStatus status;
}
```

`dto/spot/ZoneSummary.java`:
```java
package ParkingSystem.demo.dto.spot;

public record ZoneSummary(Long zoneId, long total, long available, long occupied) {}
```

`dto/spot/DashboardResponse.java`:
```java
package ParkingSystem.demo.dto.spot;

import java.util.List;

public record DashboardResponse(long total, long available, long occupied, List<ZoneSummary> byZone) {}
```

- [ ] **Step 4: Create RealtimeService stub**

`service/RealtimeService.java`:
```java
package ParkingSystem.demo.service;

import ParkingSystem.demo.entity.ParkingSpotsEntity;
import org.springframework.stereotype.Service;

@Service
public class RealtimeService {

    public void broadcastSpotUpdate(ParkingSpotsEntity spot) {
        // wired in Task 8
    }

    public void broadcastDashboard() {
        // wired in Task 8
    }
}
```

- [ ] **Step 5: Create ParkingSpotRepository**

`repository/ParkingSpotRepository.java`:
```java
package ParkingSystem.demo.repository;

import ParkingSystem.demo.entity.ParkingSpotsEntity;
import ParkingSystem.demo.enums.SpotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpotsEntity, Long> {
    List<ParkingSpotsEntity> findByZone_idId(Long zoneId);
    long countByStatus(SpotStatus status);
    long countByZone_idIdAndStatus(Long zoneId, SpotStatus status);
    long countByZone_idId(Long zoneId);
}
```

- [ ] **Step 6: Create ParkingSpotService**

`service/ParkingSpotService.java`:
```java
package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.spot.DashboardResponse;
import ParkingSystem.demo.dto.spot.SpotResponse;
import ParkingSystem.demo.dto.spot.ZoneSummary;
import ParkingSystem.demo.entity.ParkingSpotsEntity;
import ParkingSystem.demo.entity.ParkingZonesEntity;
import ParkingSystem.demo.enums.SpotStatus;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.ParkingSpotRepository;
import ParkingSystem.demo.repository.ParkingZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class ParkingSpotService {

    private final ParkingSpotRepository spotRepository;
    private final ParkingZoneService zoneService;
    private final RealtimeService realtimeService;
    private final ParkingZoneRepository zoneRepository;

    public List<SpotResponse> listByZone(Long zoneId) {
        return spotRepository.findByZone_idId(zoneId).stream().map(this::toResponse).toList();
    }

    public SpotResponse create(Long zoneId, Long row, Long col, String type) {
        ParkingZonesEntity zone = zoneService.findOrThrow(zoneId);
        ParkingSpotsEntity spot = ParkingSpotsEntity.builder()
                .row(row).col(col).type(type).status(SpotStatus.AVAILABLE).zone_id(zone).build();
        return toResponse(spotRepository.save(spot));
    }

    public SpotResponse update(Long spotId, Long row, Long col, String type) {
        ParkingSpotsEntity spot = findOrThrow(spotId);
        spot.setRow(row);
        spot.setCol(col);
        spot.setType(type);
        return toResponse(spotRepository.save(spot));
    }

    public void delete(Long spotId) {
        if (!spotRepository.existsById(spotId)) {
            throw new ResourceNotFoundException("Spot not found with id: " + spotId);
        }
        spotRepository.deleteById(spotId);
    }

    public void updateStatus(Long spotId, SpotStatus status) {
        ParkingSpotsEntity spot = findOrThrow(spotId);
        spot.setStatus(status);
        ParkingSpotsEntity saved = spotRepository.save(spot);
        realtimeService.broadcastSpotUpdate(saved);
        realtimeService.broadcastDashboard();
    }

    public DashboardResponse getDashboard() {
        long total = spotRepository.count();
        long available = spotRepository.countByStatus(SpotStatus.AVAILABLE);
        long occupied = spotRepository.countByStatus(SpotStatus.OCCUPIED);
        List<ZoneSummary> byZone = zoneRepository.findAll().stream().map(z -> {
            long zTotal = spotRepository.countByZone_idId(z.getId());
            long zAvail = spotRepository.countByZone_idIdAndStatus(z.getId(), SpotStatus.AVAILABLE);
            return new ZoneSummary(z.getId(), zTotal, zAvail, zTotal - zAvail);
        }).toList();
        return new DashboardResponse(total, available, occupied, byZone);
    }

    public void simulate(Integer count) {
        List<ParkingSpotsEntity> spots = spotRepository.findAll();
        if (spots.isEmpty()) return;
        Random rng = new Random();
        int limit = (count != null) ? Math.min(count, spots.size()) : spots.size();
        spots.stream().limit(limit).forEach(spot -> {
            SpotStatus newStatus = rng.nextBoolean() ? SpotStatus.OCCUPIED : SpotStatus.AVAILABLE;
            spot.setStatus(newStatus);
            ParkingSpotsEntity saved = spotRepository.save(spot);
            realtimeService.broadcastSpotUpdate(saved);
        });
        realtimeService.broadcastDashboard();
    }

    public ParkingSpotsEntity findOrThrow(Long id) {
        return spotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spot not found with id: " + id));
    }

    public SpotResponse toResponse(ParkingSpotsEntity s) {
        return new SpotResponse(s.getId(), s.getRow(), s.getCol(), s.getType(),
                s.getStatus(), s.getZone_id().getId());
    }
}
```

- [ ] **Step 7: Run test — expect PASS**

```bash
cd ParkingSystemBackend && ./mvnw test -Dtest=ParkingSpotServiceTest
```
Expected: `Tests run: 3, Failures: 0`

- [ ] **Step 8: Create ParkingSpotController**

`controller/ParkingSpotController.java`:
```java
package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.spot.*;
import ParkingSystem.demo.service.ParkingSpotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ParkingSpotController {

    private final ParkingSpotService spotService;

    @GetMapping("/api/zones/{zoneId}/spots")
    public ResponseEntity<List<SpotResponse>> listByZone(@PathVariable Long zoneId) {
        return ResponseEntity.ok(spotService.listByZone(zoneId));
    }

    @PostMapping("/api/zones/{zoneId}/spots")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SpotResponse> create(@PathVariable Long zoneId,
                                               @Valid @RequestBody SpotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(spotService.create(zoneId, request.getRow(), request.getCol(), request.getType()));
    }

    @PutMapping("/api/spots/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SpotResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody SpotRequest request) {
        return ResponseEntity.ok(spotService.update(id, request.getRow(), request.getCol(), request.getType()));
    }

    @DeleteMapping("/api/spots/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        spotService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/spots/dashboard")
    public ResponseEntity<DashboardResponse> dashboard() {
        return ResponseEntity.ok(spotService.getDashboard());
    }

    @PostMapping("/api/spots/webhook")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> webhook(@Valid @RequestBody SpotStatusUpdateRequest request) {
        spotService.updateStatus(request.getSpotId(), request.getStatus());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/spots/simulate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> simulate(@RequestParam(required = false) Integer count) {
        spotService.simulate(count);
        return ResponseEntity.ok().build();
    }
}
```

- [ ] **Step 9: Compile and commit**

```bash
cd ParkingSystemBackend && ./mvnw compile
git add ParkingSystemBackend/src/main/java/ParkingSystem/demo/dto/spot/ \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/service/RealtimeService.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/repository/ParkingSpotRepository.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/service/ParkingSpotService.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/controller/ParkingSpotController.java \
  ParkingSystemBackend/src/test/java/ParkingSystem/demo/service/ParkingSpotServiceTest.java
git commit -m "feat: parking spots CRUD, webhook, simulate endpoint"
```

---

## Task 5: Booking Management

**Files:**
- Create: `dto/booking/BookingRequest.java`
- Create: `dto/booking/BookingResponse.java`
- Create: `repository/BookingRepository.java`
- Create: `service/BookingService.java`
- Create: `scheduler/BookingExpirationJob.java`
- Create: `controller/BookingController.java`
- Create: `test/.../service/BookingServiceTest.java`

- [ ] **Step 1: Write failing BookingService test**

`test/.../service/BookingServiceTest.java`:
```java
package ParkingSystem.demo.service;

import ParkingSystem.demo.entity.*;
import ParkingSystem.demo.enums.BookingStatus;
import ParkingSystem.demo.enums.Role;
import ParkingSystem.demo.enums.SpotStatus;
import ParkingSystem.demo.exception.ConflictException;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private ParkingSpotService spotService;
    @InjectMocks private BookingService bookingService;

    private UserEntity user() {
        return UserEntity.builder().id(1L).fullName("Alice").email("a@x.com")
                .password("pw").role(Role.USERS).isActive(true).build();
    }

    private ParkingSpotsEntity spot() {
        var zone = ParkingZonesEntity.builder().id(1L).level(1L).type("STD").build();
        return ParkingSpotsEntity.builder().id(1L).row(1L).col(1L).type("CAR")
                .status(SpotStatus.AVAILABLE).zone_id(zone).build();
    }

    private LocalDateTime now() { return LocalDateTime.now(); }

    @Test
    void create_withAvailableSpot_createsBooking() {
        when(spotService.findOrThrow(1L)).thenReturn(spot());
        when(bookingRepository.findOverlapping(eq(1L), any(), any())).thenReturn(List.of());
        when(bookingRepository.save(any())).thenAnswer(i -> {
            var b = (BookingsEntity) i.getArgument(0);
            b = BookingsEntity.builder().id(10L).startTime(b.getStartTime())
                    .endTime(b.getEndTime()).status(b.getStatus())
                    .userId(b.getUserId()).spotId(b.getSpotId()).build();
            return b;
        });
        var result = bookingService.create(user(), 1L, now(), now().plusHours(2));
        assertThat(result.status()).isEqualTo(BookingStatus.APPROVED);
        verify(spotService).updateStatus(1L, SpotStatus.OCCUPIED);
    }

    @Test
    void create_withOverlappingBooking_throwsConflict() {
        when(spotService.findOrThrow(1L)).thenReturn(spot());
        var existing = BookingsEntity.builder().id(5L).status(BookingStatus.APPROVED)
                .startTime(now()).endTime(now().plusHours(1))
                .userId(user()).spotId(spot()).build();
        when(bookingRepository.findOverlapping(eq(1L), any(), any())).thenReturn(List.of(existing));
        assertThatThrownBy(() -> bookingService.create(user(), 1L, now(), now().plusHours(2)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cancel_approvedBooking_cancelsAndFreesSpot() {
        var b = BookingsEntity.builder().id(1L).status(BookingStatus.APPROVED)
                .startTime(now()).endTime(now().plusHours(1))
                .userId(user()).spotId(spot()).build();
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(b));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        bookingService.cancel(1L, 1L);
        verify(bookingRepository).save(argThat(bk -> bk.getStatus() == BookingStatus.CANCELLED));
        verify(spotService).updateStatus(1L, SpotStatus.AVAILABLE);
    }

    @Test
    void cancel_unknownBooking_throwsNotFound() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> bookingService.cancel(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

- [ ] **Step 2: Run test — expect FAIL**

```bash
cd ParkingSystemBackend && ./mvnw test -Dtest=BookingServiceTest
```

- [ ] **Step 3: Create booking DTOs**

`dto/booking/BookingRequest.java`:
```java
package ParkingSystem.demo.dto.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingRequest {
    @NotNull private Long spotId;
    @NotNull private LocalDateTime startTime;
    @NotNull @Future private LocalDateTime endTime;
}
```

`dto/booking/BookingResponse.java`:
```java
package ParkingSystem.demo.dto.booking;

import ParkingSystem.demo.enums.BookingStatus;

import java.time.LocalDateTime;

public record BookingResponse(Long id, Long spotId, Long userId,
                               LocalDateTime startTime, LocalDateTime endTime,
                               BookingStatus status) {}
```

- [ ] **Step 4: Create BookingRepository**

`repository/BookingRepository.java`:
```java
package ParkingSystem.demo.repository;

import ParkingSystem.demo.entity.BookingsEntity;
import ParkingSystem.demo.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<BookingsEntity, Long> {

    List<BookingsEntity> findByUserId_Id(Long userId);

    List<BookingsEntity> findByStatus(BookingStatus status);

    @Query("""
        SELECT b FROM BookingsEntity b
        WHERE b.spotId.id = :spotId
          AND b.status IN ('PENDING', 'APPROVED')
          AND b.startTime < :endTime
          AND b.endTime > :startTime
    """)
    List<BookingsEntity> findOverlapping(@Param("spotId") Long spotId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    @Query("""
        SELECT b FROM BookingsEntity b
        WHERE b.status = 'APPROVED' AND b.endTime < :now
    """)
    List<BookingsEntity> findExpired(@Param("now") LocalDateTime now);
}
```

- [ ] **Step 5: Create BookingService**

`service/BookingService.java`:
```java
package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.booking.BookingResponse;
import ParkingSystem.demo.entity.BookingsEntity;
import ParkingSystem.demo.entity.ParkingSpotsEntity;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.enums.BookingStatus;
import ParkingSystem.demo.enums.SpotStatus;
import ParkingSystem.demo.exception.ConflictException;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ParkingSpotService spotService;

    public BookingResponse create(UserEntity user, Long spotId, LocalDateTime startTime, LocalDateTime endTime) {
        ParkingSpotsEntity spot = spotService.findOrThrow(spotId);
        List<BookingsEntity> overlapping = bookingRepository.findOverlapping(spotId, startTime, endTime);
        if (!overlapping.isEmpty()) {
            throw new ConflictException("Spot " + spotId + " is already booked for this time window");
        }
        BookingsEntity booking = BookingsEntity.builder()
                .startTime(startTime).endTime(endTime)
                .status(BookingStatus.APPROVED)
                .userId(user).spotId(spot).build();
        BookingsEntity saved = bookingRepository.save(booking);
        spotService.updateStatus(spotId, SpotStatus.OCCUPIED);
        return toResponse(saved);
    }

    public List<BookingResponse> listForUser(Long userId) {
        return bookingRepository.findByUserId_Id(userId).stream()
                .map(this::checkAndExpire).toList();
    }

    public BookingResponse getById(Long bookingId, Long userId) {
        return checkAndExpire(findOrThrow(bookingId));
    }

    public void cancel(Long bookingId, Long userId) {
        BookingsEntity booking = findOrThrow(bookingId);
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        spotService.updateStatus(booking.getSpotId().getId(), SpotStatus.AVAILABLE);
    }

    public List<BookingResponse> listAll() {
        return bookingRepository.findAll().stream().map(this::toResponse).toList();
    }

    public void expireOverdue() {
        bookingRepository.findExpired(LocalDateTime.now()).forEach(b -> {
            b.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(b);
            spotService.updateStatus(b.getSpotId().getId(), SpotStatus.AVAILABLE);
        });
    }

    private BookingResponse checkAndExpire(BookingsEntity b) {
        if (b.getStatus() == BookingStatus.APPROVED && b.getEndTime().isBefore(LocalDateTime.now())) {
            b.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(b);
            spotService.updateStatus(b.getSpotId().getId(), SpotStatus.AVAILABLE);
        }
        return toResponse(b);
    }

    private BookingsEntity findOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    private BookingResponse toResponse(BookingsEntity b) {
        return new BookingResponse(b.getId(), b.getSpotId().getId(), b.getUserId().getId(),
                b.getStartTime(), b.getEndTime(), b.getStatus());
    }
}
```

- [ ] **Step 6: Create BookingExpirationJob**

`scheduler/BookingExpirationJob.java`:
```java
package ParkingSystem.demo.scheduler;

import ParkingSystem.demo.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class BookingExpirationJob {

    private final BookingService bookingService;

    @Scheduled(fixedRate = 300000)
    public void expireOverdueBookings() {
        bookingService.expireOverdue();
    }
}
```

- [ ] **Step 7: Run test — expect PASS**

```bash
cd ParkingSystemBackend && ./mvnw test -Dtest=BookingServiceTest
```
Expected: `Tests run: 4, Failures: 0`

- [ ] **Step 8: Create BookingController**

`controller/BookingController.java`:
```java
package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.booking.BookingRequest;
import ParkingSystem.demo.dto.booking.BookingResponse;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/bookings")
    @PreAuthorize("hasRole('USERS')")
    public ResponseEntity<BookingResponse> create(@AuthenticationPrincipal UserEntity user,
                                                  @Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                bookingService.create(user, request.getSpotId(), request.getStartTime(), request.getEndTime()));
    }

    @GetMapping("/bookings/my")
    @PreAuthorize("hasRole('USERS')")
    public ResponseEntity<List<BookingResponse>> myBookings(@AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(bookingService.listForUser(user.getId()));
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable Long id,
                                                      @AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(bookingService.getById(id, user.getId()));
    }

    @PatchMapping("/bookings/{id}/cancel")
    @PreAuthorize("hasRole('USERS')")
    public ResponseEntity<Void> cancel(@PathVariable Long id,
                                       @AuthenticationPrincipal UserEntity user) {
        bookingService.cancel(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookingResponse>> listAll() {
        return ResponseEntity.ok(bookingService.listAll());
    }
}
```

- [ ] **Step 9: Compile and commit**

```bash
cd ParkingSystemBackend && ./mvnw compile
git add ParkingSystemBackend/src/main/java/ParkingSystem/demo/dto/booking/ \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/repository/BookingRepository.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/service/BookingService.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/scheduler/ \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/controller/BookingController.java \
  ParkingSystemBackend/src/test/java/ParkingSystem/demo/service/BookingServiceTest.java
git commit -m "feat: booking management with auto-expiration"
```

---

## Task 6: Packages

**Files:**
- Create: `dto/pkg/PackageRequest.java`
- Create: `dto/pkg/PackageResponse.java`
- Create: `repository/PackageRepository.java`
- Create: `service/PackageService.java`
- Create: `controller/PackageController.java`

- [ ] **Step 1: Create DTOs**

`dto/pkg/PackageRequest.java`:
```java
package ParkingSystem.demo.dto.pkg;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PackageRequest {
    @NotBlank private String name;
    @NotBlank private String description;
    @NotNull @Positive private Long durations;
    @NotNull @Positive private Long price;
}
```

`dto/pkg/PackageResponse.java`:
```java
package ParkingSystem.demo.dto.pkg;

public record PackageResponse(Long id, String name, String description, Long durations, Long price) {}
```

- [ ] **Step 2: Create PackageRepository**

`repository/PackageRepository.java`:
```java
package ParkingSystem.demo.repository;

import ParkingSystem.demo.entity.PackagesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PackageRepository extends JpaRepository<PackagesEntity, Long> {
    Optional<PackagesEntity> findByName(String name);
}
```

- [ ] **Step 3: Create PackageService**

`service/PackageService.java`:
```java
package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.pkg.PackageResponse;
import ParkingSystem.demo.entity.PackagesEntity;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.PackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PackageService {

    private final PackageRepository packageRepository;

    public List<PackageResponse> listAll() {
        return packageRepository.findAll().stream().map(this::toResponse).toList();
    }

    public PackageResponse create(String name, String description, Long durations, Long price) {
        PackagesEntity pkg = PackagesEntity.builder()
                .name(name).description(description).durations(durations).price(price).build();
        return toResponse(packageRepository.save(pkg));
    }

    public PackageResponse update(Long id, String name, String description, Long durations, Long price) {
        PackagesEntity pkg = findOrThrow(id);
        pkg.setName(name); pkg.setDescription(description);
        pkg.setDurations(durations); pkg.setPrice(price);
        return toResponse(packageRepository.save(pkg));
    }

    public void delete(Long id) {
        if (!packageRepository.existsById(id)) throw new ResourceNotFoundException("Package not found: " + id);
        packageRepository.deleteById(id);
    }

    public PackagesEntity findOrThrow(Long id) {
        return packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + id));
    }

    private PackageResponse toResponse(PackagesEntity p) {
        return new PackageResponse(p.getId(), p.getName(), p.getDescription(), p.getDurations(), p.getPrice());
    }
}
```

- [ ] **Step 4: Create PackageController**

`controller/PackageController.java`:
```java
package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.pkg.PackageRequest;
import ParkingSystem.demo.dto.pkg.PackageResponse;
import ParkingSystem.demo.service.PackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    @GetMapping
    public ResponseEntity<List<PackageResponse>> list() {
        return ResponseEntity.ok(packageService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PackageResponse> create(@Valid @RequestBody PackageRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(packageService.create(req.getName(), req.getDescription(), req.getDurations(), req.getPrice()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PackageResponse> update(@PathVariable Long id, @Valid @RequestBody PackageRequest req) {
        return ResponseEntity.ok(packageService.update(id, req.getName(), req.getDescription(), req.getDurations(), req.getPrice()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        packageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 5: Compile and commit**

```bash
cd ParkingSystemBackend && ./mvnw compile
git add ParkingSystemBackend/src/main/java/ParkingSystem/demo/dto/pkg/ \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/repository/PackageRepository.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/service/PackageService.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/controller/PackageController.java
git commit -m "feat: package management"
```

---

## Task 7: Subscriptions

**Files:**
- Create: `dto/subscription/SubscriptionRequest.java`
- Create: `dto/subscription/SubscriptionResponse.java`
- Create: `repository/SubscriptionRepository.java`
- Create: `service/SubscriptionService.java`
- Create: `controller/SubscriptionController.java`

- [ ] **Step 1: Create DTOs**

`dto/subscription/SubscriptionRequest.java`:
```java
package ParkingSystem.demo.dto.subscription;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscriptionRequest {
    @NotNull private Long packageId;
}
```

`dto/subscription/SubscriptionResponse.java`:
```java
package ParkingSystem.demo.dto.subscription;

import java.util.Date;

public record SubscriptionResponse(Long id, Long userId, Long packageId,
                                    String packageName, Date startDate, Date endDate, Long price) {}
```

- [ ] **Step 2: Create SubscriptionRepository**

`repository/SubscriptionRepository.java`:
```java
package ParkingSystem.demo.repository;

import ParkingSystem.demo.entity.SubscriptionsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionsEntity, Long> {

    List<SubscriptionsEntity> findByUserId_Id(Long userId);

    @Query("SELECT s FROM SubscriptionsEntity s WHERE s.userId.id = :userId AND s.endDate >= :now")
    Optional<SubscriptionsEntity> findActiveByUserId(@Param("userId") Long userId, @Param("now") Date now);
}
```

- [ ] **Step 3: Create SubscriptionService**

`service/SubscriptionService.java`:
```java
package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.subscription.SubscriptionResponse;
import ParkingSystem.demo.entity.PackagesEntity;
import ParkingSystem.demo.entity.SubscriptionsEntity;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.exception.ConflictException;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PackageService packageService;

    public SubscriptionResponse subscribe(UserEntity user, Long packageId) {
        if (subscriptionRepository.findActiveByUserId(user.getId(), new Date()).isPresent()) {
            throw new ConflictException("User already has an active subscription");
        }
        PackagesEntity pkg = packageService.findOrThrow(packageId);
        Date start = new Date();
        Date end = Date.from(Instant.now().plus(pkg.getDurations(), ChronoUnit.DAYS));
        SubscriptionsEntity sub = SubscriptionsEntity.builder()
                .userId(user).packageName(pkg).startDate(start).endDate(end).price(pkg.getPrice()).build();
        return toResponse(subscriptionRepository.save(sub));
    }

    public SubscriptionResponse getActiveSubscription(Long userId) {
        return subscriptionRepository.findActiveByUserId(userId, new Date())
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No active subscription for user: " + userId));
    }

    public List<SubscriptionResponse> listAll() {
        return subscriptionRepository.findAll().stream().map(this::toResponse).toList();
    }

    private SubscriptionResponse toResponse(SubscriptionsEntity s) {
        return new SubscriptionResponse(s.getId(), s.getUserId().getId(),
                s.getPackageName().getId(), s.getPackageName().getName(),
                s.getStartDate(), s.getEndDate(), s.getPrice());
    }
}
```

- [ ] **Step 4: Create SubscriptionController**

`controller/SubscriptionController.java`:
```java
package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.subscription.SubscriptionRequest;
import ParkingSystem.demo.dto.subscription.SubscriptionResponse;
import ParkingSystem.demo.entity.UserEntity;
import ParkingSystem.demo.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/subscriptions")
    @PreAuthorize("hasRole('USERS')")
    public ResponseEntity<SubscriptionResponse> subscribe(@AuthenticationPrincipal UserEntity user,
                                                          @Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.subscribe(user, request.getPackageId()));
    }

    @GetMapping("/subscriptions/my")
    @PreAuthorize("hasRole('USERS')")
    public ResponseEntity<SubscriptionResponse> mySubscription(@AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(subscriptionService.getActiveSubscription(user.getId()));
    }

    @GetMapping("/admin/subscriptions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SubscriptionResponse>> listAll() {
        return ResponseEntity.ok(subscriptionService.listAll());
    }
}
```

- [ ] **Step 5: Compile and commit**

```bash
cd ParkingSystemBackend && ./mvnw compile
git add ParkingSystemBackend/src/main/java/ParkingSystem/demo/dto/subscription/ \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/repository/SubscriptionRepository.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/service/SubscriptionService.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/controller/SubscriptionController.java
git commit -m "feat: package subscriptions"
```

---

## Task 8: Real-time WebSocket

**Files:**
- Create: `dto/ws/SpotUpdateMessage.java`
- Create: `dto/ws/DashboardMessage.java`
- Create: `config/WebSocketConfig.java`
- Modify: `service/RealtimeService.java` (full implementation)

- [ ] **Step 1: Create WebSocket DTOs**

`dto/ws/SpotUpdateMessage.java`:
```java
package ParkingSystem.demo.dto.ws;

import ParkingSystem.demo.enums.SpotStatus;

public record SpotUpdateMessage(Long spotId, Long row, Long col, Long zoneId, SpotStatus status) {}
```

`dto/ws/DashboardMessage.java`:
```java
package ParkingSystem.demo.dto.ws;

import ParkingSystem.demo.dto.spot.ZoneSummary;
import java.util.List;

public record DashboardMessage(long total, long available, long occupied, List<ZoneSummary> byZone) {}
```

- [ ] **Step 2: Create WebSocketConfig**

`config/WebSocketConfig.java`:
```java
package ParkingSystem.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS();
    }
}
```

- [ ] **Step 3: Implement RealtimeService**

`service/RealtimeService.java`:
```java
package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.spot.DashboardResponse;
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
```

- [ ] **Step 4: Compile**

```bash
cd ParkingSystemBackend && ./mvnw compile
```
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add ParkingSystemBackend/src/main/java/ParkingSystem/demo/dto/ws/ \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/config/WebSocketConfig.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/service/RealtimeService.java
git commit -m "feat: real-time WebSocket with STOMP broadcast"
```

---

## Task 9: Analytics

**Files:**
- Modify: `entity/ParkingZonesEntity.java` (add lat, lng)
- Create: `dto/analytics/OccupancyResponse.java`
- Create: `dto/analytics/PeakHourResponse.java`
- Create: `dto/analytics/UtilizationResponse.java`
- Create: `service/AnalyticsService.java`
- Create: `controller/AnalyticsController.java`

- [ ] **Step 1: Add lat/lng to ParkingZonesEntity**

`entity/ParkingZonesEntity.java`:
```java
package ParkingSystem.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Table(name = "parking_zones")
public class ParkingZonesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long level;

    @Column
    private String type;

    @Column
    private Double lat;

    @Column
    private Double lng;
}
```

- [ ] **Step 2: Create analytics DTOs**

`dto/analytics/OccupancyResponse.java`:
```java
package ParkingSystem.demo.dto.analytics;

public record OccupancyResponse(Long zoneId, String from, String to, double occupancyPercent) {}
```

`dto/analytics/PeakHourResponse.java`:
```java
package ParkingSystem.demo.dto.analytics;

public record PeakHourResponse(int hour, double averageOccupancyPercent) {}
```

`dto/analytics/UtilizationResponse.java`:
```java
package ParkingSystem.demo.dto.analytics;

public record UtilizationResponse(Long zoneId, long totalSpots, long totalBookings, double utilizationPercent) {}
```

- [ ] **Step 3: Add analytics queries to BookingRepository**

Add these methods to `repository/BookingRepository.java` (using `nativeQuery = true` for PostgreSQL date extraction):
```java
@Query(value = """
    SELECT EXTRACT(HOUR FROM b.start_time) AS hour, COUNT(*) AS cnt
    FROM bookings b
    WHERE b.status IN ('APPROVED', 'EXPIRED')
    GROUP BY EXTRACT(HOUR FROM b.start_time)
    ORDER BY hour
""", nativeQuery = true)
List<Object[]> countByHourOfDay();

@Query(value = """
    SELECT ps.zone_id, COUNT(*) AS cnt
    FROM bookings b
    JOIN "parkingSpots" ps ON b.spot_id = ps.id
    WHERE b.status IN ('APPROVED', 'EXPIRED')
      AND b.start_time >= :from AND b.end_time <= :to
    GROUP BY ps.zone_id
""", nativeQuery = true)
List<Object[]> countByZoneInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

@Query(value = """
    SELECT COUNT(*) FROM bookings b
    JOIN "parkingSpots" ps ON b.spot_id = ps.id
    WHERE ps.zone_id = :zoneId AND b.status IN ('APPROVED', 'EXPIRED')
""", nativeQuery = true)
long countByZoneId(@Param("zoneId") Long zoneId);
```

- [ ] **Step 4: Create AnalyticsService**

`service/AnalyticsService.java`:
```java
package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.analytics.OccupancyResponse;
import ParkingSystem.demo.dto.analytics.PeakHourResponse;
import ParkingSystem.demo.dto.analytics.UtilizationResponse;
import ParkingSystem.demo.repository.BookingRepository;
import ParkingSystem.demo.repository.ParkingSpotRepository;
import ParkingSystem.demo.repository.ParkingZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final BookingRepository bookingRepository;
    private final ParkingSpotRepository spotRepository;
    private final ParkingZoneRepository zoneRepository;

    public List<OccupancyResponse> getOccupancy(LocalDateTime from, LocalDateTime to) {
        long totalSpots = spotRepository.count();
        if (totalSpots == 0) return List.of();
        List<Object[]> rows = bookingRepository.countByZoneInRange(from, to);
        return rows.stream().map(r -> {
            Long zoneId = (Long) r[0];
            long count = (Long) r[1];
            long zoneSpots = spotRepository.countByZone_idId(zoneId);
            double pct = zoneSpots > 0 ? (count * 100.0 / zoneSpots) : 0;
            return new OccupancyResponse(zoneId, from.toString(), to.toString(), pct);
        }).toList();
    }

    public List<PeakHourResponse> getPeakHours() {
        List<Object[]> rows = bookingRepository.countByHourOfDay();
        long maxCount = rows.stream().mapToLong(r -> (Long) r[1]).max().orElse(1L);
        return rows.stream().map(r -> {
            int hour = ((Number) r[0]).intValue();
            long count = (Long) r[1];
            return new PeakHourResponse(hour, count * 100.0 / maxCount);
        }).toList();
    }

    public List<UtilizationResponse> getUtilization() {
        return zoneRepository.findAll().stream().map(zone -> {
            long spots = spotRepository.countByZone_idId(zone.getId());
            long bookings = bookingRepository.countByZoneId(zone.getId());
            double pct = spots > 0 ? Math.min(bookings * 100.0 / spots, 100.0) : 0;
            return new UtilizationResponse(zone.getId(), spots, bookings, pct);
        }).toList();
    }
}
```

- [ ] **Step 5: Create AnalyticsController**

`controller/AnalyticsController.java`:
```java
package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.analytics.OccupancyResponse;
import ParkingSystem.demo.dto.analytics.PeakHourResponse;
import ParkingSystem.demo.dto.analytics.UtilizationResponse;
import ParkingSystem.demo.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/occupancy")
    public ResponseEntity<List<OccupancyResponse>> occupancy(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(analyticsService.getOccupancy(from, to));
    }

    @GetMapping("/peak-hours")
    public ResponseEntity<List<PeakHourResponse>> peakHours() {
        return ResponseEntity.ok(analyticsService.getPeakHours());
    }

    @GetMapping("/utilization")
    public ResponseEntity<List<UtilizationResponse>> utilization() {
        return ResponseEntity.ok(analyticsService.getUtilization());
    }
}
```

- [ ] **Step 6: Compile and commit**

```bash
cd ParkingSystemBackend && ./mvnw compile
git add ParkingSystemBackend/src/main/java/ParkingSystem/demo/entity/ParkingZonesEntity.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/dto/analytics/ \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/service/AnalyticsService.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/controller/AnalyticsController.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/repository/BookingRepository.java
git commit -m "feat: analytics endpoints — occupancy, peak hours, utilization"
```

---

## Task 10: Predictions & Recommendations

**Files:**
- Create: `dto/prediction/AvailabilityPredictionResponse.java`
- Create: `dto/recommendation/RecommendationResponse.java`
- Create: `service/PredictionService.java`
- Create: `service/RecommendationService.java`
- Create: `controller/PredictionController.java`
- Create: `controller/RecommendationController.java`

- [ ] **Step 1: Create DTOs**

`dto/prediction/AvailabilityPredictionResponse.java`:
```java
package ParkingSystem.demo.dto.prediction;

public record AvailabilityPredictionResponse(Long zoneId, String targetTime, double availabilityProbability) {}
```

`dto/recommendation/RecommendationResponse.java`:
```java
package ParkingSystem.demo.dto.recommendation;

public record RecommendationResponse(Long zoneId, String reason, long availableSpots, double predictedProbability) {}
```

- [ ] **Step 2: Add prediction query to BookingRepository**

Add to `repository/BookingRepository.java` (PostgreSQL native SQL; `EXTRACT(DOW ...)` returns 0=Sun..6=Sat, Java's `getDayOfWeek().getValue()` returns 1=Mon..7=Sun — the service maps this):
```java
@Query(value = """
    SELECT COUNT(*) FROM bookings b
    JOIN "parkingSpots" ps ON b.spot_id = ps.id
    WHERE ps.zone_id = :zoneId
      AND b.status IN ('APPROVED', 'EXPIRED')
      AND EXTRACT(DOW FROM b.start_time) = :pgDow
      AND EXTRACT(HOUR FROM b.start_time) = :hour
""", nativeQuery = true)
long countHistoricalBookings(@Param("zoneId") Long zoneId,
                              @Param("pgDow") int pgDow,
                              @Param("hour") int hour);
```

Note: convert Java's `getDayOfWeek().getValue()` (1=Mon..7=Sun) to PostgreSQL DOW (0=Sun..6=Sat) in `PredictionService`:
```java
// Java Mon=1..Sun=7 → PG Sun=0..Sat=6
int javaDow = targetTime.getDayOfWeek().getValue(); // 1=Mon
int pgDow = javaDow % 7; // Mon=1→1, ..., Sat=6→6, Sun=7→0
```

- [ ] **Step 3: Create PredictionService**

`service/PredictionService.java`:
```java
package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.prediction.AvailabilityPredictionResponse;
import ParkingSystem.demo.enums.SpotStatus;
import ParkingSystem.demo.repository.BookingRepository;
import ParkingSystem.demo.repository.ParkingSpotRepository;
import ParkingSystem.demo.repository.ParkingZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PredictionService {

    private final BookingRepository bookingRepository;
    private final ParkingSpotRepository spotRepository;
    private final ParkingZoneRepository zoneRepository;

    public AvailabilityPredictionResponse predict(Long zoneId, LocalDateTime targetTime) {
        long totalSpots = spotRepository.countByZone_idId(zoneId);
        if (totalSpots == 0) {
            return new AvailabilityPredictionResponse(zoneId, targetTime.toString(), 0.0);
        }
        int javaDow = targetTime.getDayOfWeek().getValue(); // 1=Mon..7=Sun
        int pgDow = javaDow % 7; // converts to PostgreSQL DOW: 0=Sun..6=Sat
        int hour = targetTime.getHour();
        long historicalBooked = bookingRepository.countHistoricalBookings(zoneId, pgDow, hour);
        double avgBooked = Math.min(historicalBooked, totalSpots);
        double probability = (totalSpots - avgBooked) / (double) totalSpots;
        return new AvailabilityPredictionResponse(zoneId, targetTime.toString(),
                Math.max(0.0, Math.min(1.0, probability)));
    }
}
```

- [ ] **Step 4: Create RecommendationService**

`service/RecommendationService.java`:
```java
package ParkingSystem.demo.service;

import ParkingSystem.demo.dto.recommendation.RecommendationResponse;
import ParkingSystem.demo.entity.ParkingZonesEntity;
import ParkingSystem.demo.enums.SpotStatus;
import ParkingSystem.demo.repository.ParkingSpotRepository;
import ParkingSystem.demo.repository.ParkingZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ParkingZoneRepository zoneRepository;
    private final ParkingSpotRepository spotRepository;
    private final PredictionService predictionService;

    public Optional<RecommendationResponse> recommend(Double userLat, Double userLng) {
        List<ParkingZonesEntity> zones = zoneRepository.findAll();
        LocalDateTime in30 = LocalDateTime.now().plusMinutes(30);

        if (userLat != null && userLng != null) {
            Optional<ParkingZonesEntity> nearest = zones.stream()
                    .filter(z -> z.getLat() != null && z.getLng() != null)
                    .filter(z -> spotRepository.countByZone_idIdAndStatus(z.getId(), SpotStatus.AVAILABLE) > 0)
                    .min(Comparator.comparingDouble(z -> haversine(userLat, userLng, z.getLat(), z.getLng())));
            if (nearest.isPresent()) {
                ParkingZonesEntity z = nearest.get();
                long avail = spotRepository.countByZone_idIdAndStatus(z.getId(), SpotStatus.AVAILABLE);
                double prob = predictionService.predict(z.getId(), in30).availabilityProbability();
                return Optional.of(new RecommendationResponse(z.getId(), "Nearest available zone", avail, prob));
            }
        }

        Optional<ParkingZonesEntity> leastCongested = zones.stream()
                .filter(z -> spotRepository.countByZone_idIdAndStatus(z.getId(), SpotStatus.AVAILABLE) > 0)
                .max(Comparator.comparingLong(z ->
                        spotRepository.countByZone_idIdAndStatus(z.getId(), SpotStatus.AVAILABLE)));
        if (leastCongested.isPresent()) {
            ParkingZonesEntity z = leastCongested.get();
            long avail = spotRepository.countByZone_idIdAndStatus(z.getId(), SpotStatus.AVAILABLE);
            double prob = predictionService.predict(z.getId(), in30).availabilityProbability();
            return Optional.of(new RecommendationResponse(z.getId(), "Least congested zone", avail, prob));
        }

        return zones.stream()
                .max(Comparator.comparingDouble(z ->
                        predictionService.predict(z.getId(), in30).availabilityProbability()))
                .map(z -> {
                    long avail = spotRepository.countByZone_idIdAndStatus(z.getId(), SpotStatus.AVAILABLE);
                    double prob = predictionService.predict(z.getId(), in30).availabilityProbability();
                    return new RecommendationResponse(z.getId(), "Best predicted availability", avail, prob);
                });
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
```

- [ ] **Step 5: Create PredictionController**

`controller/PredictionController.java`:
```java
package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.prediction.AvailabilityPredictionResponse;
import ParkingSystem.demo.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/predict")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;

    @GetMapping("/availability")
    public ResponseEntity<AvailabilityPredictionResponse> predict(
            @RequestParam Long zoneId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime targetTime) {
        return ResponseEntity.ok(predictionService.predict(zoneId, targetTime));
    }
}
```

- [ ] **Step 6: Create RecommendationController**

`controller/RecommendationController.java`:
```java
package ParkingSystem.demo.controller;

import ParkingSystem.demo.dto.recommendation.RecommendationResponse;
import ParkingSystem.demo.exception.ResourceNotFoundException;
import ParkingSystem.demo.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/spot")
    public ResponseEntity<RecommendationResponse> recommend(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {
        return recommendationService.recommend(lat, lng)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("No zones available"));
    }
}
```

- [ ] **Step 7: Compile**

```bash
cd ParkingSystemBackend && ./mvnw compile
```
Expected: `BUILD SUCCESS`

- [ ] **Step 8: Run all tests**

```bash
cd ParkingSystemBackend && ./mvnw test
```
Expected: all tests pass

- [ ] **Step 9: Commit**

```bash
git add ParkingSystemBackend/src/main/java/ParkingSystem/demo/dto/prediction/ \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/dto/recommendation/ \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/service/PredictionService.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/service/RecommendationService.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/controller/PredictionController.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/controller/RecommendationController.java \
  ParkingSystemBackend/src/main/java/ParkingSystem/demo/repository/BookingRepository.java
git commit -m "feat: predictions and smart recommendations"
```

---

## Summary of All Endpoints

| Phase | Method | Path | Role |
|---|---|---|---|
| 1 | GET | /api/users/me | any |
| 1 | PUT | /api/users/me | any |
| 1 | GET | /api/admin/users | ADMIN |
| 1 | PATCH | /api/admin/users/{id}/role | ADMIN |
| 1 | DELETE | /api/admin/users/{id} | ADMIN |
| 2 | GET | /api/zones | any |
| 2 | POST | /api/zones | ADMIN |
| 2 | PUT | /api/zones/{id} | ADMIN |
| 2 | DELETE | /api/zones/{id} | ADMIN |
| 2 | GET | /api/zones/{id}/spots | any |
| 2 | POST | /api/zones/{id}/spots | ADMIN |
| 2 | PUT | /api/spots/{id} | ADMIN |
| 2 | DELETE | /api/spots/{id} | ADMIN |
| 2 | GET | /api/spots/dashboard | any |
| 2 | POST | /api/spots/webhook | ADMIN |
| 2 | POST | /api/spots/simulate | ADMIN |
| 3 | POST | /api/bookings | USERS |
| 3 | GET | /api/bookings/my | USERS |
| 3 | GET | /api/bookings/{id} | any |
| 3 | PATCH | /api/bookings/{id}/cancel | USERS |
| 3 | GET | /api/admin/bookings | ADMIN |
| 4 | GET | /api/packages | any |
| 4 | POST | /api/packages | ADMIN |
| 4 | PUT | /api/packages/{id} | ADMIN |
| 4 | DELETE | /api/packages/{id} | ADMIN |
| 4 | POST | /api/subscriptions | USERS |
| 4 | GET | /api/subscriptions/my | USERS |
| 4 | GET | /api/admin/subscriptions | ADMIN |
| 5 | WS | /ws (STOMP) | — |
| 5 | TOPIC | /topic/spots | — |
| 5 | TOPIC | /topic/dashboard | — |
| 6 | GET | /api/analytics/occupancy | ADMIN |
| 6 | GET | /api/analytics/peak-hours | ADMIN |
| 6 | GET | /api/analytics/utilization | ADMIN |
| 6 | GET | /api/predict/availability | any |
| 6 | GET | /api/recommend/spot | any |
