# Authentication & Authorization Design

**Date:** 2026-04-26
**Scope:** Register, Login, Email Verification, JWT, BCrypt

---

## Context

Spring Boot 4.0.4 · Java 17 · PostgreSQL · Spring Security (already in pom.xml) · Lombok.
No controllers, services, or repositories exist yet — only entities and enums.
Existing `UserEntity` has: `id`, `fullName`, `email`, `password`, `role` (enum: ADMIN, USERS).

---

## Package Structure

New packages under `ParkingSystem/demo/`:

```
controller/
  AuthController.java

service/
  AuthService.java
  JwtService.java
  EmailService.java

repository/
  UserRepository.java

dto/
  RegisterRequest.java
  LoginRequest.java
  AuthResponse.java

security/
  JwtAuthFilter.java
  SecurityConfig.java
  UserDetailsServiceImpl.java
```

---

## UserEntity Changes

Add two columns to the existing `users` table:

| Field             | Type      | Constraints                        |
|-------------------|-----------|------------------------------------|
| `isActive`        | boolean   | NOT NULL, default false            |
| `verificationToken` | String  | nullable, cleared after verification |

`UserEntity` implements `UserDetails` directly. `getAuthorities()` returns the user's `Role`.
Hibernate `ddl-auto=update` adds the columns automatically on next startup.

---

## Endpoints

### POST /api/auth/register
- **Request:** `{ "fullName": "", "email": "", "password": "" }`
- **Actions:** validate → check email uniqueness → BCrypt hash password → save user (`isActive=false`, `role=USERS`, UUID `verificationToken`) → send verification email
- **Response 201:** `{ "message": "Registration successful. Please check your email to verify your account." }`
- **Error 409:** email already registered

### GET /api/auth/verify?token=\<uuid\>
- **Actions:** find user by `verificationToken` → set `isActive=true` → clear `verificationToken` → save
- **Response 200:** `{ "message": "Account verified successfully. You can now log in." }`
- **Error 400:** token not found or already used

### POST /api/auth/login
- **Request:** `{ "email": "", "password": "" }`
- **Actions:** load user → verify BCrypt password → check `isActive=true` → generate 24h JWT
- **Response 200:** `{ "token": "<jwt>" }`
- **Error 401:** wrong credentials
- **Error 403:** account not yet verified

---

## JWT & Security

### JwtService
- Library: `io.jsonwebtoken` (JJWT 0.12.6), HMAC-SHA256
- Secret: env var `JWT_SECRET` (Base64-encoded, min 256-bit)
- Claims: `sub` (email), `role`, `iat`, `exp` (24h)
- Methods: `generateToken(UserEntity)`, `extractEmail(String)`, `isTokenValid(String, UserDetails)`

### UserDetailsServiceImpl
- Implements Spring Security's `UserDetailsService`
- `loadUserByUsername(email)` → finds `UserEntity` by email, throws `UsernameNotFoundException` if not found
- `UserEntity` implements `UserDetails` directly, so it is returned as-is

### JwtAuthFilter (OncePerRequestFilter)
- Reads `Authorization: Bearer <token>` header
- Extracts email → loads UserDetails → validates → sets `UsernamePasswordAuthenticationToken` in SecurityContextHolder
- Skips `/api/auth/**`

### SecurityConfig
- **Permitted:** `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/verify`
- **Protected:** all other endpoints require valid JWT
- **Session policy:** STATELESS
- **CSRF:** disabled
- **Beans:** `BCryptPasswordEncoder`, `AuthenticationManager`, `SecurityFilterChain`

---

## Email Flow

- Library: `spring-boot-starter-mail` with `JavaMailSender`
- On register: send HTML email to user with verification link
- Link format: `http://<APP_BASE_URL>/api/auth/verify?token=<uuid>`

---

## Environment Variables

### New vars to add to `.env`:
```
MAIL_HOST=smtp.gmail.com
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM_EMAIL=your@gmail.com
JWT_SECRET=your-base64-256bit-secret
APP_BASE_URL=http://localhost:8080
```

### Additions to `application.properties`:
```properties
spring.mail.host=${MAIL_HOST}
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

app.jwt.secret=${JWT_SECRET}
app.jwt.expiration=86400000

app.base-url=${APP_BASE_URL}
```

---

## New Dependencies (pom.xml)

```xml
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-api</artifactId>
  <version>0.12.6</version>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-impl</artifactId>
  <version>0.12.6</version>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-jackson</artifactId>
  <version>0.12.6</version>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```
