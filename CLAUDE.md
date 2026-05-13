# CLAUDE.md — MediFlow Platform

This file is the authoritative implementation log for Claude (and any LLM).
Every time a new feature, file, or change is made, it must be recorded here.
Read this before generating any code — it prevents duplication and drift.

---

## Tech Stack

### Backend (`platform/` — Spring Boot)

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 (Spring Framework 7.x) |
| Persistence | Spring Data JPA + Hibernate + PostgreSQL |
| Validation | Jakarta Bean Validation (spring-boot-starter-validation) |
| Security | Spring Security (stateless, CSRF disabled, all /api/** public) |
| API Docs | springdoc-openapi 2.8.8 (Swagger UI at /swagger-ui/index.html) |
| Boilerplate | Lombok (@Builder, @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor) |
| Build | Maven |
| Dev | spring-boot-devtools |

**Database:** PostgreSQL on localhost:5432, database name `hsp`
**DDL mode:** `spring.jpa.hibernate.ddl-auto: update` — Hibernate manages schema

---

### Frontend (`hspui/` — React SaaS Dashboard)

| Layer | Technology |
|---|---|
| Framework | React 18 (Create React App) |
| Routing | React Router DOM v6 — nested routes, `<Outlet>`, `useNavigate` |
| Styling | Tailwind CSS v3 — `darkMode: 'class'`, CSS custom property color aliases |
| Animation | Framer Motion v11 — `motion.*`, `AnimatePresence`, `variants`, `layoutId` |
| Icons | Lucide React |
| Charts | Recharts — `AreaChart`, `BarChart`, `PieChart`, custom tooltips |
| State | React hooks only (`useState`, `useMemo`, `useEffect`, `useContext`) |
| Dev server | CRA dev server on `localhost:3000`; proxies `/api/**` to `localhost:8080` |

**Theme:** Dark-first SaaS aesthetic (Stripe/Linear/Vercel inspired); `localStorage` key `mf-theme`
**Proxy note:** `"proxy": "http://localhost:8080"` in `package.json` — Spring Boot must be running for API calls; `/favicon.ico` proxy errors in console are harmless when backend is offline.

#### Tailwind Color Aliases (CSS custom properties)

| Alias | CSS var | Purpose |
|---|---|---|
| `bg-page` | `--c-page` | Outermost page background |
| `bg-sidebar` | `--c-sidebar` | Sidebar background |
| `bg-card` | `--c-card` | Card / panel background |
| `bg-surface` | `--c-surface` | Input / subtle surface |
| `border-border` | `--c-border` | All borders |
| `text-tx1` | `--c-tx1` | Primary text |
| `text-tx2` | `--c-tx2` | Secondary text |
| `text-tx3` | `--c-tx3` | Muted / label text |

Dark values live in `index.css` under `.dark {}`. Light values live under `:root {}`. Recharts does **not** support CSS vars in stroke/fill — read `useTheme()` and pass explicit hex strings.

---

## Frontend File Structure (`hspui/src/`)

```
hspui/
├── public/                         CRA public assets
├── src/
│   ├── index.js                    Entry — mounts <App /> with ThemeProvider
│   ├── index.css                   Tailwind directives + CSS custom property theme vars
│   ├── App.jsx                     BrowserRouter + Routes (login + nested dashboard)
│   │
│   ├── contexts/
│   │   └── ThemeContext.jsx        Dark/light toggle; persists to localStorage 'mf-theme'
│   │
│   ├── components/layout/
│   │   ├── Layout.jsx              Shell: manages sidebar collapse, page transitions
│   │   ├── Sidebar.jsx             Collapsible 240px↔64px; layoutId nav indicator
│   │   └── Navbar.jsx              Sticky glass header; notifications + profile dropdowns
│   │
│   ├── pages/
│   │   ├── Login/
│   │   │   ├── LoginPage.jsx       Glassmorphism login; static (no auth API yet)
│   │   │   └── LoginPage.css       All login-specific keyframes + CSS vars
│   │   ├── Dashboard.jsx           KPI counters, AreaChart, PieChart, recent table
│   │   ├── Patients.jsx            Search + filter + pagination + Add modal
│   │   ├── Doctors.jsx             Summary stats, filter tabs, 3-col card grid
│   │   ├── Appointments.jsx        Status filter tabs, appointment cards, Book modal
│   │   └── Billing.jsx             Revenue KPIs, dual-series AreaChart, dept BarChart, invoice table
│   │
│   └── utils/
│       ├── apiService.js           Fetch wrapper; unwraps ApiResponse envelope; token helpers
│       └── errorHandler.js         parseApiError() → { message, errors, status }
│
├── tailwind.config.js              darkMode:'class'; custom color aliases; custom shadows
├── postcss.config.js               tailwindcss + autoprefixer
└── package.json                    proxy → localhost:8080
```

**Routes:**
- `/login` → `LoginPage` (standalone, no Layout)
- `/dashboard` → `Dashboard`
- `/patients` → `Patients`
- `/doctors` → `Doctors`
- `/appointments` → `Appointments`
- `/billing` → `Billing`
- `/` and `/*` → redirect to `/dashboard`

---

## Backend Package Structure

Base package: `com.mediflow.platform`

```
platform/
├── appointment/          ← Appointment module (see Change Log #2)
│   ├── controller/       AppointmentController.java
│   ├── dto/              AppointmentRequestDTO.java, AppointmentResponseDTO.java
│   ├── entity/           Appointment.java
│   ├── enums/            AppointmentStatus.java, ConsultationType.java, BookedBy.java
│   ├── exception/        AppointmentNotFoundException.java, AppointmentConflictException.java
│   ├── mapper/           AppointmentMapper.java
│   ├── repository/       AppointmentRepository.java
│   └── service/          AppointmentService.java
│       └── impl/         AppointmentServiceImpl.java
│
├── common/
│   ├── exception/        ResourceNotFoundException.java, ResourceAlreadyExistsException.java
│   │                     BusinessRuleViolationException.java, GlobalExceptionHandler.java
│   └── response/         ApiResponse.java
│
├── config/               SecurityConfig.java, SwaggerConfig.java
│
├── doctor/               ← Doctor module (see Change Log #1)
│   ├── controller/       DoctorController.java
│   ├── dto/              DoctorRequestDTO.java, DoctorResponseDTO.java
│   ├── entity/           Doctor.java
│   ├── enums/            DoctorStatus.java
│   ├── exception/        DoctorNotFoundException.java, DoctorAlreadyExistsException.java
│   ├── mapper/           DoctorMapper.java
│   ├── repository/       DoctorRepository.java
│   └── service/          DoctorService.java
│       └── impl/         DoctorServiceImpl.java
│
├── patient/              ← Patient module (see Change Log #1)
│   ├── controller/       PatientController.java
│   ├── dto/              PatientRequestDTO.java, PatientResponseDTO.java
│   ├── entity/           Patient.java
│   ├── enums/            BloodGroup.java, Gender.java, PatientStatus.java
│   ├── exception/        PatientNotFoundException.java, PatientAlreadyExistsException.java
│   ├── mapper/           PatientMapper.java
│   ├── repository/       PatientRepository.java
│   └── service/          PatientService.java
│       └── impl/         PatientServiceImpl.java
│
└── MediflowPlatformApplication.java
```

---

## Architecture Patterns (apply to every module)

- **Feature-based packaging** — code grouped by domain, not by layer
- **DTO isolation** — entities never exposed in API responses; always mapped to `*ResponseDTO`
- **Static mapper classes** — `DoctorMapper`, `PatientMapper`, `AppointmentMapper` use static methods, no MapStruct
- **Soft delete only** — records are never hard-deleted; status set to `INACTIVE`
- **`ApiResponse<T>` wrapper** — every controller response uses this unified envelope
- **Exception hierarchy** — `ResourceNotFoundException` (404), `ResourceAlreadyExistsException` (409), `BusinessRuleViolationException` (422); `GlobalExceptionHandler` maps all
- **Transactions** — write methods use `@Transactional`; read methods use `@Transactional(readOnly = true)`
- **Audit fields** — every business entity extends `BaseAuditEntity` which provides `createdAt`, `updatedAt` (JPA `@CreatedDate`/`@LastModifiedDate`), `createdBy`, and `updatedBy` (JPA `@CreatedBy`/`@LastModifiedBy`). Values are populated automatically via `SecurityAuditorAware` — the authenticated user's email from the JWT SecurityContext. **Never accept audit fields from client payloads.**
- **Pagination** — controllers accept `page` (default 0) and `size` (default 10, capped at 100); sorted `DESC` by `createdAt` or domain-specific date fields
- **Lombok @Builder.Default** — all collection fields and enum defaults must use `@Builder.Default` to avoid Lombok ignoring initializers

---

## HTTP Status Mapping

| Situation | HTTP Status | Exception |
|---|---|---|
| Resource not found | 404 | `ResourceNotFoundException` (and subclasses) |
| Duplicate / conflict | 409 | `ResourceAlreadyExistsException` (and subclasses) |
| Business rule violated | 422 | `BusinessRuleViolationException` |
| Validation failure | 400 | `MethodArgumentNotValidException` / `ConstraintViolationException` |
| Bad request body | 400 | `HttpMessageNotReadableException` |
| Wrong HTTP method | 405 | `HttpRequestMethodNotSupportedException` |
| Unknown route | 404 | `NoResourceFoundException` |
| Unexpected error | 500 | `Exception` (catch-all) |

Note: `HttpStatus.UNPROCESSABLE_ENTITY` is deprecated in Spring Framework 7.x — use `.status(422)` raw integer.

---

## Change Log

Changes are recorded newest-first. Every future implementation must add an entry here.

---

### [#7] Dashboard Stats API + Frontend Field-Name Fix — 2026-05-12

**Type:** New feature (backend) + Bug fix (frontend)

**Summary:** Built the `/api/v1/dashboard/stats` endpoint so the dashboard KPI cards (Total Patients, Active Doctors, Today's Appointments) show live counts. Fixed all field-name mismatches in the frontend Recent Appointments table that were causing blank rows despite data being returned.

#### Backend — New Files (4)

| File | Purpose |
|---|---|
| `dashboard/dto/DashboardStatsDTO.java` | Response DTO: `totalPatients`, `activeDoctors`, `todayAppointments`, `monthlyRevenue` (long), plus `*Trend` (double) and `*Sub` (String) fields for each KPI card |
| `dashboard/service/DashboardService.java` | Service interface: `getStats()` |
| `dashboard/service/impl/DashboardServiceImpl.java` | Queries `patientRepository.count()`, `doctorRepository.countByStatus(ACTIVE)`, `appointmentRepository.countByAppointmentDate(LocalDate.now())`; `monthlyRevenue` fixed at 0 until billing module is built |
| `dashboard/controller/DashboardController.java` | `GET /api/v1/dashboard/stats` → `ApiResponse<DashboardStatsDTO>` |

#### Backend — Modified Files (3)

| File | Change |
|---|---|
| `patient/repository/PatientRepository.java` | Added `long countByStatus(PatientStatus status)` |
| `doctor/repository/DoctorRepository.java` | Added `long countByStatus(DoctorStatus status)` |
| `appointment/repository/AppointmentRepository.java` | Added `long countByAppointmentDate(LocalDate date)` |

#### Frontend — Modified Files (1)

| File | Change |
|---|---|
| `hspui/src/pages/Dashboard.jsx` | Fixed Recent Appointments table: `a.code→a.appointmentCode`, `a.patient→a.patientName`, `a.doctor→a.doctorName`, removed `a.specialty` (not in DTO), `a.time→a.startTime`, `a.type→a.consultationType`, `a.status→a.appointmentStatus`; also fixed `key` prop on `<motion.tr>` |

---

### [#6] Enterprise Appointment Booking UX + Available-Slots API — 2026-05-12

**Type:** Feature — full-stack redesign of appointment booking workflow

**Summary:** Replaced the raw CRUD booking form with a workflow-driven, enterprise-grade appointment experience. Manual code entry fields (patientCode, doctorCode, bookedBy) are eliminated from the frontend. The backend now derives `bookedBy` from the authenticated JWT principal and exposes a new `/available-slots` endpoint for dynamic slot selection. A full-text search API was added to both patient and doctor modules to power live searchable comboboxes.

#### Backend — New Files (1)

| File | Purpose |
|---|---|
| `appointment/dto/TimeSlotDTO.java` | Response DTO for available slot data: `startTime` (HH:mm), `endTime` (HH:mm), `available` (boolean) |

#### Backend — Modified Files (12)

| File | Change |
|---|---|
| `patient/repository/PatientRepository.java` | Added `searchActivePatients(@Param("q") String q, Pageable)` JPQL query — full-text across name, patientCode, phoneNumber, email; filters to ACTIVE status |
| `doctor/repository/DoctorRepository.java` | Added `searchActiveDoctors(@Param("q") String q, Pageable)` JPQL query — matches name, specialization, doctorCode; filters to ACTIVE status |
| `patient/service/PatientService.java` | Added `searchPatients(String query, int page, int size)` to interface |
| `patient/service/impl/PatientServiceImpl.java` | Implemented `searchPatients()` delegating to `searchActivePatients` — sorted ASC by firstName |
| `patient/controller/PatientController.java` | `GET /api/v1/patients` now accepts optional `search` param; delegates to `searchPatients()` when provided |
| `doctor/service/DoctorService.java` | Added `searchDoctors(String query, int page, int size)` to interface |
| `doctor/service/impl/DoctorServiceImpl.java` | Implemented `searchDoctors()` delegating to `searchActiveDoctors` — sorted ASC by firstName |
| `doctor/controller/DoctorController.java` | `GET /api/v1/doctors` now accepts optional `search` param; delegates to `searchDoctors()` when provided |
| `appointment/dto/AppointmentRequestDTO.java` | `bookedBy` field: removed `@NotNull` — field is now optional; backend derives it from SecurityContext |
| `appointment/repository/AppointmentRepository.java` | Added `findActiveByDoctorAndDate(Long doctorId, LocalDate date)` JPQL query — returns non-cancelled/no-show appointments for slot availability calculation |
| `appointment/service/AppointmentService.java` | Added `getAvailableSlots(String doctorCode, LocalDate date)` method |
| `appointment/service/impl/AppointmentServiceImpl.java` | (1) Added `deriveBookedBy()` private helper — reads `SecurityContextHolder`, maps ROLE_ADMIN→ADMIN, ROLE_PATIENT→PATIENT, else SYSTEM. (2) `bookAppointment()` now calls `deriveBookedBy()` when `request.getBookedBy() == null`. (3) Implemented `getAvailableSlots()` — generates 30-min slots 09:00–17:30, marks each available/booked based on `findActiveByDoctorAndDate` overlap check. |
| `appointment/controller/AppointmentController.java` | Added `GET /api/v1/appointments/available-slots?doctorCode=...&date=...` endpoint |

#### Available Slots Spec

- Slot window: **09:00 – 17:30** in 30-minute increments (17 slots/day)
- A slot is `available=false` when any non-cancelled, non-no-show appointment overlaps it (`startTime < slotEnd AND endTime > slotStart`)
- Past dates are rejected with 422 `BusinessRuleViolationException`
- Inactive/on-leave doctors are rejected with 422

#### BookedBy Derivation Logic (server-side, JWT-only)

| JWT Role | `bookedBy` value |
|---|---|
| `ROLE_ADMIN` | `ADMIN` |
| `ROLE_PATIENT` | `PATIENT` |
| `ROLE_DOCTOR` / anonymous / system | `SYSTEM` |

#### Frontend — Modified Files (1)

| File | Change |
|---|---|
| `hspui/src/pages/Appointments.jsx` | Complete redesign of `BookModal`. See details below. |

#### Frontend Booking UX — What Changed

**Removed:**
- Manual `patientCode` text input (PAT-XXXXXXXXXX)
- Manual `doctorCode` text input (DOC-YYYY-NNNN)
- `bookedBy` dropdown (Admin / Patient / System)
- Manual `startTime` / `endTime` time inputs

**Added:**
- `PatientSearch` combobox — debounced (350ms) live search via `GET /api/v1/patients?search=...`; shows name, patientCode, phone in dropdown; selected patient renders a confirmation card
- `DoctorSearch` combobox — same pattern; shows name, specialization, fee; ACTIVE badge on results
- `SlotPicker` component — 4-column grid of 30-min slots fetched from `/available-slots` when both doctor and date are set; available/booked/selected visual states with spring animations; loading skeleton while fetching
- `SuccessModal` — animated check circle, appointment code (prominent), patient/doctor/date/time/type/fee summary; shown after successful booking
- `FormSection` wrapper — labelled sections (Patient, Doctor, Appointment Details, Available Time Slots, Visit Details) for visual workflow structure
- Progressive slot reveal — slot grid animates in (`AnimatePresence` height expand) only after doctor + date are both selected
- Client-side validation — checks patient, doctor, date, and slot before submitting; inline error messages per field

**Contract change:**
- Frontend sends `{ patientCode, doctorCode, appointmentDate, startTime (HH:mm:ss), endTime (HH:mm:ss), consultationType, reasonForVisit, notes }` — `bookedBy` is intentionally omitted

---

### [#5] Centralized JPA Auditing — JWT-backed createdBy / updatedBy — 2026-05-12

**Type:** Cross-cutting infrastructure — audit traceability across all business entities

**Summary:** Implemented enterprise-grade centralized auditing using Spring Data JPA's `@EnableJpaAuditing` infrastructure. All business entities now automatically record `createdBy` and `updatedBy` (authenticated user's email extracted from the JWT SecurityContext) alongside the existing `createdAt` / `updatedAt` timestamps. Audit identity is exclusively server-controlled — client payloads can never influence audit fields.

#### New Files Created (2)

| File | Purpose |
|---|---|
| `common/audit/BaseAuditEntity.java` | `@MappedSuperclass` + `@EntityListeners(AuditingEntityListener.class)`; declares `createdAt` (`@CreatedDate`), `updatedAt` (`@LastModifiedDate`), `createdBy` (`@CreatedBy`, `updatable=false`), `updatedBy` (`@LastModifiedBy`). All business entities extend this. |
| `common/audit/SecurityAuditorAware.java` | `AuditorAware<String>` implementation (`@Component("securityAuditorAware")`). Reads `Authentication` from `SecurityContextHolder`; casts principal to `UserPrincipal` and returns `email` (no DB lookup). Falls back to `"system"` for bootstrap / unauthenticated contexts. |

#### Existing Files Modified (12)

| File | Change |
|---|---|
| `MediflowPlatformApplication.java` | Added `@EnableJpaAuditing(auditorAwareRef = "securityAuditorAware")` |
| `auth/security/UserPrincipal.java` | Added `email` field (from `user.getEmail()`) so `SecurityAuditorAware` can resolve the auditor without an extra DB query |
| `auth/entity/User.java` | Extends `BaseAuditEntity`; removed own `@CreationTimestamp createdAt` and `@UpdateTimestamp updatedAt` fields; removed Hibernate timestamp imports |
| `patient/entity/Patient.java` | Same — extends `BaseAuditEntity`, Hibernate timestamp fields removed |
| `doctor/entity/Doctor.java` | Same — extends `BaseAuditEntity`, Hibernate timestamp fields removed |
| `appointment/entity/Appointment.java` | Same — extends `BaseAuditEntity`, Hibernate timestamp fields removed |
| `patient/dto/PatientResponseDTO.java` | Added `createdBy` and `updatedBy` String fields |
| `doctor/dto/DoctorResponseDTO.java` | Added `createdBy` and `updatedBy` String fields |
| `appointment/dto/AppointmentResponseDTO.java` | Added `createdBy` and `updatedBy` String fields |
| `patient/mapper/PatientMapper.java` | `toResponseDTO()` now maps `createdBy` and `updatedBy` from entity |
| `doctor/mapper/DoctorMapper.java` | `toResponseDTO()` now maps `createdBy` and `updatedBy` from entity |
| `appointment/mapper/AppointmentMapper.java` | `toResponseDTO()` now maps `createdBy` and `updatedBy` from entity |

#### Audit Flow

```
User logs in → JWT issued with email claim
    ↓
User calls POST /api/v1/doctors (ADMIN only)
    ↓
JwtAuthenticationFilter validates token, populates SecurityContextHolder
with UserPrincipal (contains email, e.g. admin@mediflow.com)
    ↓
DoctorServiceImpl.createDoctor() calls doctorRepository.save(doctor)
    ↓
AuditingEntityListener fires before INSERT
    ↓
SecurityAuditorAware.getCurrentAuditor() extracts admin@mediflow.com
    ↓
Hibernate sets:
  created_by = 'admin@mediflow.com'
  updated_by = 'admin@mediflow.com'
  created_at = <now>
  updated_at = <now>
    ↓
Response DTO includes all four audit fields
```

#### Fallback Identity

| Scenario | Auditor value |
|---|---|
| Normal JWT-authenticated request | User's email (e.g. `doctor1@mediflow.com`) |
| `DataInitializer` bootstrap at startup | `"system"` |
| Anonymous / unauthenticated request | `"system"` |
| Unknown principal type | `"system"` (+ WARN log) |

#### DB Schema Changes (ddl-auto: update)

`created_by VARCHAR(255)` and `updated_by VARCHAR(255)` columns added to:
`users`, `patients`, `doctors`, `appointments`

Existing rows will have NULL for these two columns (nullable=true intentional — pre-audit rows must not fail schema migration).

#### Security Guarantee

Audit fields are declared only on `BaseAuditEntity` (server side).
No request DTO contains `createdBy` or `updatedBy`.
`@CreatedBy` / `@LastModifiedBy` are populated exclusively by `AuditingEntityListener` via `SecurityAuditorAware` — the SecurityContext is the only source of truth.

---

### [#4] Auth Module — JWT Security, RBAC, Session Management — 2026-05-11

**Type:** New feature — complete authentication and authorization system

**Summary:** Implemented a centralized, stateless JWT-based authentication and RBAC system. All users (ADMIN, DOCTOR, PATIENT) authenticate through a single `/api/v1/auth/login` endpoint. Doctor and Patient creation now auto-creates a linked User account with a temporary password.

#### New Files Created (29)

| File | Purpose |
|---|---|
| `auth/enums/UserStatus.java` | `ACTIVE, INACTIVE, LOCKED` |
| `auth/enums/RoleName.java` | `ADMIN, DOCTOR, PATIENT` |
| `auth/entity/Role.java` | JPA entity, table `roles` |
| `auth/entity/User.java` | JPA entity, table `users`; username/email/phone unique; ManyToMany roles (EAGER) |
| `auth/entity/RefreshToken.java` | JPA entity, table `refresh_tokens`; token rotation + soft revocation |
| `auth/repository/RoleRepository.java` | `findByName()`, `existsByName()` |
| `auth/repository/UserRepository.java` | `findByUsername()`, `findByEmail()`, existence checks |
| `auth/repository/RefreshTokenRepository.java` | `revokeAllByUserId()`, `deleteExpiredOrRevokedByUserId()` via JPQL |
| `auth/dto/LoginRequestDTO.java` | `usernameOrEmail` + `password` |
| `auth/dto/AuthResponseDTO.java` | `accessToken`, `refreshToken`, `tokenType`, `expiresIn`, `roles`, `username` |
| `auth/dto/RefreshTokenRequestDTO.java` | `refreshToken` field |
| `auth/exception/AuthException.java` | Base auth exception → 401 |
| `auth/exception/InvalidCredentialsException.java` | Bad username/password → 401 |
| `auth/exception/AccountLockedException.java` | LOCKED account → 401 |
| `auth/exception/AccountInactiveException.java` | INACTIVE account → 401 |
| `auth/exception/InvalidTokenException.java` | Expired/revoked/malformed token → 401 |
| `auth/jwt/JwtUtil.java` | JJWT 0.12.x: generate/validate/parse; claims: `sub`=username, `uid`=userId, `roles`; `isCloseToExpiry()` for sliding session |
| `auth/security/UserPrincipal.java` | `UserDetails` wrapper carrying `userId`; maps roles to `ROLE_` prefix |
| `auth/security/UserDetailsServiceImpl.java` | Loads `UserPrincipal` from DB by username; used by filter + auth provider |
| `auth/filter/JwtAuthenticationFilter.java` | `OncePerRequestFilter`; extracts Bearer token; validates; populates `SecurityContextHolder`; issues `X-New-Token` header when token is within 15 min of expiry |
| `auth/handler/AuthEntryPoint.java` | 401 JSON responses for unauthenticated access |
| `auth/handler/AuthAccessDeniedHandler.java` | 403 JSON responses for insufficient role |
| `auth/service/AuthService.java` | Interface: `login()`, `refreshToken()`, `logout()` |
| `auth/service/impl/AuthServiceImpl.java` | Full implementation with BCrypt, token rotation, `deriveUniqueUsername()`, `generateTemporaryPassword()` |
| `auth/controller/AuthController.java` | `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout` |
| `auth/config/DataInitializer.java` | `ApplicationRunner`; seeds default admin user with BCrypt password at startup |
| `doctor/dto/DoctorCreationResponseDTO.java` | Wraps `DoctorResponseDTO` + `temporaryPassword` + `username` |
| `patient/dto/PatientCreationResponseDTO.java` | Wraps `PatientResponseDTO` + `temporaryPassword` + `username` |
| `src/main/resources/data.sql` | Idempotent role seeding (`ON CONFLICT DO NOTHING`) for ADMIN/DOCTOR/PATIENT |

#### Existing Files Modified (12)

| File | Change |
|---|---|
| `pom.xml` | Added `jjwt-api`, `jjwt-impl`, `jjwt-jackson` 0.12.6 |
| `src/main/resources/application.yaml` | Added `app.jwt.*` config (secret, access/refresh expiration); added `spring.jpa.defer-datasource-initialization: true` and `spring.sql.init.mode: always` |
| `config/SecurityConfig.java` | Complete rewrite — `@EnableMethodSecurity`; JWT filter; `DaoAuthenticationProvider`; `BCryptPasswordEncoder(12)`; `AuthenticationManager` bean; path-level RBAC rules |
| `doctor/entity/Doctor.java` | Added `@ManyToOne(nullable=true) User user` FK field |
| `patient/entity/Patient.java` | Added `@ManyToOne(nullable=true) User user` FK field |
| `doctor/service/DoctorService.java` | `createDoctor()` return type changed to `DoctorCreationResponseDTO` |
| `doctor/service/impl/DoctorServiceImpl.java` | `createDoctor()` now creates User account (DOCTOR role + temp password) atomically with doctor entity |
| `doctor/controller/DoctorController.java` | `POST /api/v1/doctors` returns `DoctorCreationResponseDTO` |
| `patient/service/PatientService.java` | `createPatient()` return type changed to `PatientCreationResponseDTO` |
| `patient/service/impl/PatientServiceImpl.java` | `createPatient()` now creates User account (PATIENT role + temp password) atomically with patient entity |
| `patient/controller/PatientController.java` | `POST /api/v1/patients` returns `PatientCreationResponseDTO` |
| `common/exception/GlobalExceptionHandler.java` | Added handlers for `AuthException`, `InvalidTokenException`, `AccountLockedException`, `AccountInactiveException`, `BadCredentialsException`, `DisabledException`, `LockedException`, `AccessDeniedException` |

#### Auth Package Structure

```
auth/
├── config/         DataInitializer.java
├── controller/     AuthController.java
├── dto/            LoginRequestDTO, AuthResponseDTO, RefreshTokenRequestDTO
├── entity/         User, Role, RefreshToken
├── enums/          UserStatus, RoleName
├── exception/      AuthException (base), InvalidCredentials, AccountLocked, AccountInactive, InvalidToken
├── filter/         JwtAuthenticationFilter.java
├── handler/        AuthEntryPoint, AuthAccessDeniedHandler
├── jwt/            JwtUtil.java
├── repository/     UserRepository, RoleRepository, RefreshTokenRepository
├── security/       UserPrincipal, UserDetailsServiceImpl
└── service/        AuthService.java
    └── impl/       AuthServiceImpl.java
```

#### Token Strategy

| Token | Type | TTL | Storage |
|---|---|---|---|
| Access Token | Signed JWT (HS256) | 1 hour | Client memory / localStorage |
| Refresh Token | Opaque UUID string | 7 days | DB (`refresh_tokens` table) |

#### Sliding Session

- JWT filter detects tokens within **15 minutes** of expiry and issues a new token in `X-New-Token` response header.
- Client-initiated refresh: `POST /api/v1/auth/refresh` exchanges refresh token for new access + refresh token pair (token rotation).
- Session expired response: `{ "success": false, "message": "Session expired. Please login again." }` with HTTP 401.

#### Default Bootstrap Credentials

| Field | Value |
|---|---|
| Username | `admin` |
| Password | `Admin@123` |
| Email | `admin@mediflow.com` |
| Role | ADMIN |

**Change in production** — `DataInitializer` warns at startup if running with default credentials.

#### RBAC Summary (path-level in SecurityConfig)

| Role | Permitted Actions |
|---|---|
| ADMIN | All endpoints including POST/DELETE doctors & patients |
| DOCTOR | GET all, PUT appointments & profiles |
| PATIENT | GET endpoints only |
| All authenticated | POST /api/v1/auth/logout |
| Public | POST /auth/login, POST /auth/refresh, Swagger UI |

Fine-grained ownership validation (patient can only see own record) is enforced in service layer via `jwtUtil.extractUserId()` comparison.

#### DB Tables Added by Hibernate (ddl-auto: update)

| Table | Description |
|---|---|
| `users` | Central identity table |
| `roles` | Fixed role definitions (seeded via data.sql) |
| `user_roles` | ManyToMany join table |
| `refresh_tokens` | Refresh token store with revocation flag |
| `doctors.user_id` | New nullable FK column (added to existing table) |
| `patients.user_id` | New nullable FK column (added to existing table) |

---

### [#3] React Frontend — Premium SaaS Dashboard — 2026-05-11

**Type:** New — complete frontend application (`hspui/`)

**Summary:** Built the full React 18 + Tailwind CSS + Framer Motion SaaS dashboard UI for MediFlow. Dark-first enterprise aesthetic modelled on Stripe/Linear/Vercel. All pages use mock data; wiring to the backend APIs is a TODO once auth is implemented.

#### New Files Created

| File | Purpose |
|---|---|
| `hspui/tailwind.config.js` | `darkMode:'class'`; CSS-var color aliases; custom shadows (`card`, `glow-indigo`, `modal`) |
| `hspui/postcss.config.js` | Tailwind + autoprefixer PostCSS pipeline |
| `hspui/src/index.css` | Tailwind directives; `:root` (light) and `.dark` (dark) CSS custom property sets; `.glass`, `.skeleton` utilities |
| `hspui/src/contexts/ThemeContext.jsx` | Theme context — `theme`, `toggleTheme`; persists to `localStorage` key `mf-theme` |
| `hspui/src/components/layout/Layout.jsx` | App shell — sidebar collapse state, `AnimatePresence` page transitions keyed on `location.pathname` |
| `hspui/src/components/layout/Sidebar.jsx` | Collapsible sidebar 240px↔64px via `motion.aside`; `layoutId="nav-indicator"` sliding active bar; mobile drawer with backdrop |
| `hspui/src/components/layout/Navbar.jsx` | Sticky glass navbar; notifications dropdown with unread dot + mark-all-read; profile dropdown; Sun/Moon theme toggle with `AnimatePresence` |
| `hspui/src/pages/Dashboard.jsx` | `useCountUp` hook (rAF + cubic ease-out); 4 KPI cards; 7-month AreaChart; donut PieChart; recent appointments table |
| `hspui/src/pages/Patients.jsx` | Client-side search + status filter + pagination (PAGE_SIZE=6); `AddPatientModal` with full form |
| `hspui/src/pages/Doctors.jsx` | Summary stats bar; search + filter; 3-col responsive card grid with dept/qual tiles |
| `hspui/src/pages/Appointments.jsx` | Filter tabs with counts; `AnimatePresence mode="popLayout"`; appointment cards with time, type, status; `BookModal` |
| `hspui/src/pages/Billing.jsx` | 4 revenue KPI cards; dual-series AreaChart (Revenue + dashed Expenses); horizontal BarChart by dept with per-bar Cell colors; invoice table with PAID/PENDING/OVERDUE filter |
| `hspui/src/utils/apiService.js` | `request()` fetch wrapper; unwraps `ApiResponse` envelope; exports `get`, `post`, `put`, `del`, `setAuthToken`, `clearAuthToken`, `isAuthenticated` |
| `hspui/src/utils/errorHandler.js` | `parseApiError(error)` → `{ message, errors, status }`; handles network errors, 400 field map, 404/409/422/500 |
| `hspui/src/pages/Login/LoginPage.jsx` | Glassmorphism login; 4 background orbs + dot-grid; dual pulsing logo rings; 3-col stats; static submit with `navigate('/dashboard')` |
| `hspui/src/pages/Login/LoginPage.css` | 9 animation keyframes; `--lp-accent: #7DD3FC`; all login-page scoped styles |

#### Existing Files Modified

| File | Change |
|---|---|
| `hspui/package.json` | Added 7 runtime deps (react-router-dom, framer-motion, lucide-react, recharts, tailwindcss, autoprefixer, postcss); added `"proxy": "http://localhost:8080"` |
| `hspui/src/index.js` | Changed `import './App.css'` → `import './index.css'` |
| `hspui/src/App.jsx` | Full rewrite — `ThemeProvider` + `BrowserRouter` + `Routes`; login route standalone; all dashboard pages nested under `<Layout />` |

#### Key Design Decisions

- **CSS variable theming** — Tailwind color aliases map to CSS vars (`var(--c-card)` etc.); toggling `dark` class on `<html>` switches the entire palette with zero per-element `dark:` variants
- **Recharts hex workaround** — Recharts cannot read CSS vars in SVG attributes; `useTheme()` is used to pass explicit hex strings for `gridColor` and `tickColor`
- **Sidebar width sync** — `Layout` holds `collapsed` boolean; computes `sw = collapsed ? 64 : 240`; passes to both `<Sidebar>` and `<Navbar>`; `motion.main animate={{ paddingLeft: sw }}` keeps content in sync with the same Framer transition
- **Animated counters** — `useCountUp(end, duration)` custom hook; `requestAnimationFrame` loop with `1 - Math.pow(1 - progress, 3)` cubic ease-out
- **Login → dashboard** — Login is static (no auth API yet); on submit it simulates a 1.8 s delay then calls `navigate('/dashboard')`; wire to `apiService.post('/api/v1/auth/login', ...)` once the auth module is ready

#### TODO — Not Yet Implemented

- Auth module on the backend (no `/api/v1/auth/login` endpoint exists yet)
- Reports page (`/reports` — nav item present, no component)
- Notifications page (`/notifications` — nav item present, no component)
- Settings page (`/settings` — nav item present, no component)
- Replace all mock data arrays with real `apiService` calls once backend endpoints are consumed

---

### [#2] Appointment Module — 2026-05-08

**Type:** New feature — full domain module

**Summary:** Implemented the Appointment module as the transactional bridge between Patient and Doctor. This is a workflow engine, not simple CRUD — it enforces a status state machine, doctor schedule conflict detection, historical snapshot integrity, and deactivation guards on both Patient and Doctor.

#### New Files Created (14)

| File | Purpose |
|---|---|
| `appointment/enums/AppointmentStatus.java` | `SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW` |
| `appointment/enums/ConsultationType.java` | `IN_PERSON, ONLINE, FOLLOW_UP` |
| `appointment/enums/BookedBy.java` | `ADMIN, PATIENT, SYSTEM` |
| `appointment/exception/AppointmentNotFoundException.java` | Extends `ResourceNotFoundException` → 404 |
| `appointment/exception/AppointmentConflictException.java` | Extends `ResourceAlreadyExistsException` → 409 |
| `appointment/entity/Appointment.java` | JPA entity, table `appointments`, 17+ fields |
| `appointment/repository/AppointmentRepository.java` | JPA repo with JPQL overlap query |
| `appointment/dto/AppointmentRequestDTO.java` | Booking request with Bean Validation |
| `appointment/dto/AppointmentResponseDTO.java` | Response DTO with snapshot fields |
| `appointment/mapper/AppointmentMapper.java` | Static mapper: `toEntity()`, `toResponseDTO()` |
| `appointment/service/AppointmentService.java` | Service interface (6 methods) |
| `appointment/service/impl/AppointmentServiceImpl.java` | Full implementation with business rules |
| `appointment/controller/AppointmentController.java` | REST controller, base path `/api/v1/appointments` |
| `common/exception/BusinessRuleViolationException.java` | Shared 422 exception for all modules |

#### Existing Files Modified (5)

| File | Change |
|---|---|
| `patient/entity/Patient.java` | Added `@OneToMany(mappedBy="patient", fetch=LAZY)` + `@Builder.Default` back-reference to `Appointment` |
| `doctor/entity/Doctor.java` | Added `@OneToMany(mappedBy="doctor", fetch=LAZY)` + `@Builder.Default` back-reference to `Appointment` |
| `patient/service/impl/PatientServiceImpl.java` | Injected `AppointmentRepository`; added future-appointment guard to `deactivatePatient()` |
| `doctor/service/impl/DoctorServiceImpl.java` | Injected `AppointmentRepository`; added future-appointment guard to `deactivateDoctor()` |
| `common/exception/GlobalExceptionHandler.java` | Added `@ExceptionHandler(BusinessRuleViolationException.class)` → HTTP 422 |

#### Appointment Entity Key Design Decisions

- **Snapshot fields** (`doctorNameSnapshot`, `patientNameSnapshot`, `consultationFeeSnapshot`) — frozen at booking time; changing a doctor's fee or name never alters historical records
- **No cascade** on `@OneToMany` in Patient/Doctor — appointments have independent lifecycle
- **`appointmentStatus` default** = `SCHEDULED` via `@Builder.Default`
- **`consultationFeeSnapshot`** uses `BigDecimal(precision=10, scale=2)` — monetary precision

#### Appointment Code Format

- Pattern: `APT-YYYY-NNNN` (e.g., `APT-2026-0001`)
- Year-scoped sequential, resets each calendar year
- Queries DB for highest existing code with current year prefix, parses sequence, increments
- Limit: 9999/year — throws `IllegalStateException` if exceeded
- DB unique constraint is the concurrency safety net for single-instance deployments

#### API Endpoints

| Method | Path | Action | Response |
|---|---|---|---|
| `POST` | `/api/v1/appointments` | Book appointment | 201 CREATED |
| `GET` | `/api/v1/appointments/{code}` | Get by code | 200 OK |
| `GET` | `/api/v1/appointments/patient/{code}` | List by patient (paginated) | 200 OK |
| `GET` | `/api/v1/appointments/doctor/{code}` | List by doctor (paginated) | 200 OK |
| `PUT` | `/api/v1/appointments/{code}/cancel` | Cancel (SCHEDULED only) | 200 OK |
| `PUT` | `/api/v1/appointments/{code}/complete` | Complete (IN_PROGRESS only) | 200 OK |

#### Status Transition State Machine

```
SCHEDULED  →  CANCELLED  (via /cancel)
SCHEDULED  →  IN_PROGRESS  (future: /start — not yet implemented)
IN_PROGRESS → COMPLETED  (via /complete)
All other transitions → 422 BusinessRuleViolationException
```

#### Business Rules Enforced in `bookAppointment()`

1. Patient must exist → 404
2. Patient must be `ACTIVE` → 422
3. Doctor must exist → 404
4. Doctor must be `ACTIVE` (blocks `ON_LEAVE` and `INACTIVE`) → 422
5. `appointmentDate` must be today or future → 422
6. `startTime` must be before `endTime` → 422
7. No overlapping doctor appointments (JPQL interval overlap: `s1 < e2 AND e1 > s2`, excludes `CANCELLED`/`NO_SHOW`) → 409
8. Generate `APT-YYYY-NNNN` code
9. Populate snapshots, save, return DTO

#### Deactivation Guards (added to existing services)

- `PatientServiceImpl.deactivatePatient()` — blocks deactivation if patient has future `SCHEDULED` or `IN_PROGRESS` appointments
- `DoctorServiceImpl.deactivateDoctor()` — same guard for doctors

---

### [#1] Patient & Doctor Modules — (initial implementation, pre-2026-05-08)

**Type:** Foundation — first two domain modules

**Summary:** Established the full architecture pattern for all future modules.

#### Patient Module Files

| File | Purpose |
|---|---|
| `patient/entity/Patient.java` | JPA entity, table `patients`; fields: patientCode, firstName, lastName, dateOfBirth, gender, bloodGroup, phoneNumber, email, addressLine1/2, city, state, postalCode, emergencyContactName, emergencyContactPhone, allergies, medicalHistory, status |
| `patient/enums/Gender.java` | `MALE, FEMALE, OTHER` |
| `patient/enums/BloodGroup.java` | `A_POSITIVE, A_NEGATIVE, B_POSITIVE, B_NEGATIVE, O_POSITIVE, O_NEGATIVE, AB_POSITIVE, AB_NEGATIVE` |
| `patient/enums/PatientStatus.java` | `ACTIVE, INACTIVE` |
| `patient/exception/PatientNotFoundException.java` | Extends `ResourceNotFoundException` → 404 |
| `patient/exception/PatientAlreadyExistsException.java` | Extends `ResourceAlreadyExistsException` → 409 |
| `patient/repository/PatientRepository.java` | JPA repo; `findByPatientCode`, `existsByEmail`, `existsByEmailAndPatientCodeNot`, `existsByPatientCode`, `findAllByStatus` |
| `patient/dto/PatientRequestDTO.java` | Create/update request with Bean Validation |
| `patient/dto/PatientResponseDTO.java` | Response DTO |
| `patient/mapper/PatientMapper.java` | Static: `toEntity()`, `toResponseDTO()`, `updateEntityFromDTO()` |
| `patient/service/PatientService.java` | Interface: `createPatient`, `getPatientByCode`, `getAllPatients`, `updatePatient`, `deactivatePatient` |
| `patient/service/impl/PatientServiceImpl.java` | Implementation |
| `patient/controller/PatientController.java` | Base path `/api/v1/patients` |

**Patient code format:** `PAT-` + 10 random alphanumeric chars from UUID (e.g., `PAT-A1B2C3D4E5`)

#### Doctor Module Files

| File | Purpose |
|---|---|
| `doctor/entity/Doctor.java` | JPA entity, table `doctors`; fields: doctorCode, firstName, lastName, email, phoneNumber, specialization, qualification, yearsOfExperience, consultationFee (BigDecimal), licenseNumber, department, status |
| `doctor/enums/DoctorStatus.java` | `ACTIVE, INACTIVE, ON_LEAVE` |
| `doctor/exception/DoctorNotFoundException.java` | Extends `ResourceNotFoundException` → 404 |
| `doctor/exception/DoctorAlreadyExistsException.java` | Extends `ResourceAlreadyExistsException` → 409 |
| `doctor/repository/DoctorRepository.java` | JPA repo; `findByDoctorCode`, `existsByEmail`, `existsByLicenseNumber`, `existsByEmailAndDoctorCodeNot`, `existsByLicenseNumberAndDoctorCodeNot`, `findTopByDoctorCodeStartingWithOrderByDoctorCodeDesc`, `findAllByStatus` |
| `doctor/dto/DoctorRequestDTO.java` | Create/update request |
| `doctor/dto/DoctorResponseDTO.java` | Response DTO |
| `doctor/mapper/DoctorMapper.java` | Static: `toEntity()`, `toResponseDTO()`, `updateEntityFromDTO()` |
| `doctor/service/DoctorService.java` | Interface: `createDoctor`, `getDoctorByCode`, `getAllDoctors`, `updateDoctor`, `deactivateDoctor` |
| `doctor/service/impl/DoctorServiceImpl.java` | Implementation |
| `doctor/controller/DoctorController.java` | Base path `/api/v1/doctors` |

**Doctor code format:** `DOC-YYYY-NNNN` — year-scoped sequential (e.g., `DOC-2026-0001`)

#### Common Infrastructure

| File | Purpose |
|---|---|
| `common/exception/ResourceNotFoundException.java` | Base 404 exception |
| `common/exception/ResourceAlreadyExistsException.java` | Base 409 exception |
| `common/exception/GlobalExceptionHandler.java` | `@RestControllerAdvice` mapping all exceptions to `ApiResponse` |
| `common/response/ApiResponse.java` | Unified response envelope with `success`, `message`, `data`, `errors` fields |
| `config/SecurityConfig.java` | CSRF disabled, stateless session, all `/api/**` routes public |
| `config/SwaggerConfig.java` | OpenAPI group for `/api/v1/**`; Swagger UI enabled |

---

## Rules for Future Changes

### Backend Rules

1. **Add a change log entry** for every new file, every modified file, and every deleted file.
2. **Do not modify** `GlobalExceptionHandler` without updating the HTTP status mapping table above.
3. **Do not expose** entity classes in controller return types — always go through a `*ResponseDTO`.
4. **Always use** `@Builder.Default` on any Lombok `@Builder` field that has an initializer (collection or enum default).
5. **Never use** `HttpStatus.UNPROCESSABLE_ENTITY` — use `.status(422)` directly (deprecated in Spring Framework 7.x).
6. **New domain modules** must follow the exact same package layout as `patient/` and `doctor/`.
7. **Business code formats** — document the format pattern and year-boundary behavior in the change log entry.

### Frontend Rules

8. **Add a change log entry** for every new page, component, utility, or config change in `hspui/`.
9. **Never use Tailwind `dark:` variants** on individual elements — all theming is done via CSS custom properties on `:root` / `.dark`; toggling the `dark` class on `<html>` is the only mechanism.
10. **Never pass CSS variables to Recharts** `stroke` / `fill` / `tick` props — Recharts renders to SVG where CSS vars don't resolve; always read `useTheme()` and pass explicit hex strings.
11. **New pages** must be added to the `<Routes>` in `App.jsx` and to the `NAV_ITEMS` array in `Sidebar.jsx`.
12. **Mock data** field names must mirror the corresponding backend DTO field names so the switch to real API calls is a drop-in replacement.
13. **Framer Motion** — use `variants` + `staggerChildren` on container/item pairs for consistent page-entry animations; do not add one-off inline `initial`/`animate` props when a variant already exists.
14. **Login page** — keep all login styles in `LoginPage.css` (scoped to `.lp-*` class prefix); do not bleed Tailwind utilities into the login page — it predates the Tailwind setup and uses its own CSS pipeline.
