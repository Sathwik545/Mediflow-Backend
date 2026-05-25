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
│   │   ├── Consultations/
│   │   │   ├── ConsultationList.jsx      Consultation overview; stat strip; status tabs; paginated cards
│   │   │   └── ConsultationWorkspace.jsx Clinical encounter workspace; vitals, clinical, Rx, follow-up
│   │   ├── Dashboard.jsx           KPI counters, AreaChart, PieChart, recent table
│   │   ├── Patients.jsx            Search + filter + pagination + Add modal
│   │   ├── Doctors.jsx             Summary stats, filter tabs, 3-col card grid
│   │   ├── Appointments.jsx        Status filter tabs, appointment cards, Book modal; Open Consultation button
│   │   ├── Billing.jsx             Revenue KPIs, dual-series AreaChart, dept BarChart, invoice table
│   │   └── Reports/
│   │       ├── LabReports.jsx      Lab orders list; stat cards; search + filter tabs; paginated table
│   │       └── LabReportDetail.jsx Lab order detail workspace; tests, results, file uploads, verify
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
- `/consultations` → `ConsultationList`
- `/consultations/:consultationCode` → `ConsultationWorkspace`
- `/billing` → `Billing`
- `/invoices` → `Invoices`
- `/settings` → `HospitalSettings`
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
├── consultation/         ← Consultation module (see Change Log #12)
│   ├── controller/       ConsultationController.java
│   ├── dto/              ConsultationSaveDTO.java, ConsultationResponseDTO.java
│   │                     PrescriptionItemRequestDTO.java, PrescriptionItemResponseDTO.java
│   ├── entity/           Consultation.java, PrescriptionItem.java
│   ├── enums/            ConsultationStatus.java
│   ├── exception/        ConsultationNotFoundException.java, ConsultationAlreadyExistsException.java
│   ├── mapper/           ConsultationMapper.java
│   ├── repository/       ConsultationRepository.java
│   └── service/          ConsultationService.java
│       └── impl/         ConsultationServiceImpl.java
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
├── lab/                  ← Lab Reports module (see Change Log #13)
│   ├── controller/       LabOrderController.java, LabReportController.java
│   ├── dto/              LabOrderRequestDTO, LabOrderItemRequestDTO, LabOrderItemResponseDTO
│   │                     LabOrderResponseDTO, LabOrderStatusUpdateDTO
│   │                     LabReportRequestDTO, LabReportUpdateDTO, LabReportResponseDTO
│   │                     ReportAttachmentResponseDTO
│   ├── entity/           LabOrder.java, LabOrderItem.java, LabReport.java, ReportAttachment.java
│   ├── enums/            LabOrderPriority.java, LabOrderStatus.java, ReportStatus.java
│   ├── exception/        LabOrderNotFoundException.java, LabReportNotFoundException.java
│   │                     InvalidReportFileException.java
│   ├── mapper/           LabOrderMapper.java, LabReportMapper.java
│   ├── repository/       LabOrderRepository.java, LabOrderItemRepository.java
│   │                     LabReportRepository.java, ReportAttachmentRepository.java
│   └── service/          LabOrderService.java, LabReportService.java
│       └── impl/         LabOrderServiceImpl.java, LabReportServiceImpl.java
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

### [#14] Lab Report PDF Generation — Enterprise Downloadable Medical Reports — 2026-05-25

**Type:** New feature — backend PDF generation + frontend preview/download buttons

**Summary:** Implemented enterprise-grade, dynamically generated lab report PDFs. PDFs are generated in-memory on every request using OpenPDF 1.3.43 (same library as the invoice module) — never stored on disk or in the database. The feature adds View PDF (inline browser preview) and Download PDF (browser save dialog) buttons to the LabReportDetail workspace. Full RBAC is enforced server-side on every PDF request.

#### Design Contract

- **Generated PDF is NEVER stored.** It is rendered on each request and streamed directly as `application/pdf` bytes. No blob in DB, no file on filesystem.
- **Two co-existing file types per report:** (1) uploaded raw attachments (existing, untouched); (2) generated HMS report PDF (new).
- **Snapshot fidelity:** Two new nullable columns `patient_name_snapshot` and `doctor_name_snapshot` are added to `lab_reports`. Populated on `createLabReport()` — frozen at that moment. PDFs use the snapshot if present; fall back to live entity names for pre-feature records. Nullable so existing rows are not broken.
- **RBAC model (enforced in service, not just URL):**
  - ADMIN → any report PDF
  - DOCTOR → own patients' reports only (doctor.user.email == JWT email)
  - PATIENT → own VERIFIED reports only (PENDING/READY → 403)
- **Defense-in-depth:** every PDF request re-validates ownership and VERIFIED status even for direct URL access.
- **Hospital settings:** loaded from `HospitalSettingsService.getSettings()` at PDF generation time. If settings are unavailable (unconfigured system), PDF renders with a fallback hospital name and no branding — never crashes.
- **Content-Disposition:** `?download=true` → `attachment` (save dialog); default → `inline` (browser tab preview).
- **Cache-Control:** `no-store, no-cache, must-revalidate` on every PDF response.

#### PDF Structure

| Section | Contents |
|---|---|
| Hospital Header (teal band) | Hospital name, address, phone, email, website, GST; "LAB REPORT" label + report code on right |
| Report Metadata (4-col grid) | Report code, order code, report date, status (colour-coded), last updated, test name |
| Patient & Doctor (2-col) | Patient: name (snapshot), code, gender, DOB, age (calculated), blood group; Doctor: name (snapshot), code, specialization, consultation reference |
| Test Results (table) | Test name, category, result (red if abnormal + ABNORMAL badge), reference range, remarks; "No test results recorded" row if no result yet |
| Clinical Interpretation | Paragraph section; omitted entirely if `interpretation` is null/blank |
| Verification (3-col) | Status badge (VERIFIED/READY/PENDING, colour-coded), verified by (email), verified at (datetime); digital signature placeholder box; authorizing doctor name |
| Footer | Computer-generated disclaimer, confidentiality notice, report timestamp, hospital code + support email |

#### New Backend Files (1)

| File | Purpose |
|---|---|
| `lab/pdf/LabReportPdfService.java` | `@Component`; pure PDF generation from `LabReport` + `HospitalSettingsResponseDTO`; uses OpenPDF 1.3.x `com.lowagie.text.*` API (same as `InvoicePdfService`); colors via `java.awt.Color`; all sections gracefully handle null/missing fields; returns `byte[]` |

#### Modified Backend Files (4)

| File | Change |
|---|---|
| `lab/entity/LabReport.java` | Added `patientNameSnapshot VARCHAR(255)` (nullable) and `doctorNameSnapshot VARCHAR(255)` (nullable); populated in `createLabReport()` at report creation time |
| `lab/service/LabReportService.java` | Added `ResponseEntity<byte[]> generateLabReportPdf(String reportCode, boolean download)` to interface |
| `lab/service/impl/LabReportServiceImpl.java` | (1) Added `LabReportPdfService` and `HospitalSettingsService` field injections. (2) `createLabReport()` now sets `patientNameSnapshot` and `doctorNameSnapshot` from live entity at creation time. (3) Implemented `generateLabReportPdf()` with RBAC via `validatePdfAccess()`. (4) Added `validatePdfAccess()` private helper (mirrors `validateDownloadAccess()` with extra audit logging). (5) Added `resolveCallerRole()` for log context. |
| `lab/controller/LabReportController.java` | Added `GET /{reportCode}/pdf?download=false` endpoint with full Swagger `@Operation` doc |

#### Modified Frontend Files (1)

| File | Change |
|---|---|
| `hspui/src/pages/Reports/LabReportDetail.jsx` | (1) Added `FileBadge` to lucide-react imports. (2) Added `PdfActionButtons` sub-component (View PDF + Download PDF buttons with per-button loading state, error display via AnimatePresence, lock message when not VERIFIED). (3) Wired `PdfActionButtons` into each test row body, below the attachments section, separated by a `border-t` divider. |

#### API Endpoint Added

| Method | Path | Role | Param | Action |
|---|---|---|---|---|
| `GET` | `/api/v1/lab-reports/{reportCode}/pdf` | ANY AUTHENTICATED (RBAC in service) | `?download=true\|false` | Generate + stream lab report PDF |

#### Security

- URL-level: covered by existing `GET /api/v1/** → authenticated()` catch-all in `SecurityConfig` — no SecurityConfig change required.
- Service-level `validatePdfAccess()`: ADMIN passes immediately; DOCTOR validates `doctor.user.email == JWT email`; PATIENT validates `report.status == VERIFIED` AND `patient.user.email == JWT email`.
- Unauthorized attempts are logged with `[Security]` prefix, caller email, and report code.

#### DB Schema Changes (ddl-auto: update)

| Table | Column Added |
|---|---|
| `lab_reports` | `patient_name_snapshot VARCHAR(255)` (nullable) |
| `lab_reports` | `doctor_name_snapshot VARCHAR(255)` (nullable) |

Existing rows have NULL in both columns — PDF service falls back to live entity names for them.

#### Edge Cases Handled

| Edge Case | Handling |
|---|---|
| Patient accessing PENDING/READY report PDF | 403 AccessDeniedException; logged |
| Doctor accessing another doctor's patient PDF | 403 AccessDeniedException; logged |
| Report not found | 404 LabReportNotFoundException |
| HospitalSettings not configured | PDF renders with fallback name; WARN logged; never throws |
| Missing logo | Not rendered in PDF (no image loading in this version) |
| Null/blank interpretation | Entire interpretation section omitted from PDF |
| No result recorded yet | Test results table shows "No test results recorded." row |
| Abnormal result | Result cell highlighted red; "⚠ ABNORMAL" appended in result column |
| Long remarks / interpretation | OpenPDF auto-wraps within cell; `setLeading(13f)` ensures multi-line readability |
| Frontend: 403 on PDF request | "You do not have access to this report PDF." message rendered inline |
| Frontend: 404 on PDF request | "Report PDF unavailable." message rendered inline |
| Frontend: multiple rapid clicks | Each button independently disabled while its own request is in-flight |
| Frontend: report not VERIFIED | "PDF available once report is verified" lock message (no buttons shown) |
| Concurrent PDF generation | Fully stateless; each request independent; thread-safe by design |

#### Frontend PDF Pattern (same as Invoices.jsx)

```javascript
// Fetch PDF blob with JWT auth header (window.open cannot send auth)
const resp = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
const blob = await resp.blob();
const blobUrl = URL.createObjectURL(blob);

// Preview: open new tab; revoke after 60s
window.open(blobUrl, '_blank', 'noopener,noreferrer');
setTimeout(() => URL.revokeObjectURL(blobUrl), 60000);

// Download: programmatic anchor click
const a = document.createElement('a');
a.href = blobUrl; a.download = `LAB-REPORT-${reportCode}.pdf`; a.click();
URL.revokeObjectURL(blobUrl);
```

---

### [#13] Lab Reports Module — Enterprise Diagnostic Workflow — 2026-05-20

**Type:** New feature — full-stack lab reports module (backend + frontend)

**Summary:** Implemented a complete enterprise-grade lab diagnostic workflow integrated with the consultation lifecycle. Doctors order lab tests from a consultation, lab technicians upload report files and enter results, doctors verify reports, and patients can download their verified reports. Files are stored on the filesystem (not in PostgreSQL) and served via authenticated streaming endpoints.

#### Design Contract

- **Lab orders are NOT standalone CRUD.** A lab order must be linked to an existing consultation; patient and doctor are derived from the consultation, never supplied by the client.
- **One lab order → many lab order items** (one per test). A lab report corresponds to exactly one lab order item (enforced by unique constraint).
- **File storage:** Files stored under `uploads/reports/` (configurable via `app.upload.directory`). DB stores only metadata (fileName, fileUrl, fileType, fileSize). Files are never stored as PostgreSQL blobs.
- **Report status lifecycle:** `PENDING` → `READY` (auto when resultValue added or file uploaded) → `VERIFIED` (explicit doctor action).
- **PATIENT download restriction:** Patients may only download attachments from VERIFIED reports.
- **DOCTOR ownership:** Doctors can only manage lab orders they created (validated via `consultation.doctor.user.email == JWT email`).
- **File validation:** Max 10 MB; allowed MIME types: `application/pdf`, `image/png`, `image/jpeg`; extension blacklist enforced on top of MIME check.

#### Code Formats

- Lab orders: `LAB-YYYY-NNNN` (e.g., `LAB-2026-0001`) — year-scoped sequential
- Lab reports: `REP-YYYY-NNNN` (e.g., `REP-2026-0001`) — year-scoped sequential

#### New Backend Files (26)

| File | Purpose |
|---|---|
| `lab/enums/LabOrderPriority.java` | `NORMAL, URGENT, STAT` |
| `lab/enums/LabOrderStatus.java` | `ORDERED, SAMPLE_COLLECTED, IN_PROGRESS, COMPLETED, CANCELLED` |
| `lab/enums/ReportStatus.java` | `PENDING, READY, VERIFIED` |
| `lab/entity/LabOrder.java` | JPA entity, table `lab_orders`; extends `BaseAuditEntity`; ManyToOne consultation/appointment/patient/doctor; unique `labOrderCode`; OneToMany items (cascade ALL, orphanRemoval) |
| `lab/entity/LabOrderItem.java` | JPA entity, table `lab_order_items`; ManyToOne labOrder; testCode, testName, category, remarks |
| `lab/entity/LabReport.java` | JPA entity, table `lab_reports`; unique constraints on reportCode and (lab_order_id, lab_order_item_id); result fields; reportStatus default PENDING; OneToMany attachments |
| `lab/entity/ReportAttachment.java` | JPA entity, table `report_attachments`; ManyToOne labReport; fileName (server UUID-based), originalFileName, fileType, fileSize, fileUrl (relative path) |
| `lab/exception/LabOrderNotFoundException.java` | Extends `ResourceNotFoundException` → 404 |
| `lab/exception/LabReportNotFoundException.java` | Extends `ResourceNotFoundException` → 404 |
| `lab/exception/InvalidReportFileException.java` | Extends `BusinessRuleViolationException` → 422 |
| `lab/dto/LabOrderRequestDTO.java` | `@NotBlank consultationCode`, `@NotNull priority`, clinicalNotes, instructions, `@NotEmpty @Valid List<LabOrderItemRequestDTO> items` |
| `lab/dto/LabOrderItemRequestDTO.java` | `@NotBlank testName`, optional testCode, category, remarks |
| `lab/dto/LabOrderItemResponseDTO.java` | id, testCode, testName, category, remarks, hasReport, reportCode, reportStatus |
| `lab/dto/LabOrderResponseDTO.java` | Full response with labOrderCode, patient/doctor, consultation, priority, status, items, audit fields |
| `lab/dto/LabOrderStatusUpdateDTO.java` | `@NotNull LabOrderStatus status` |
| `lab/dto/LabReportRequestDTO.java` | `@NotBlank labOrderCode`, `@NotNull Long labOrderItemId`, result fields |
| `lab/dto/LabReportUpdateDTO.java` | All optional: resultValue, referenceRange, abnormalFlag, interpretation, remarks |
| `lab/dto/LabReportResponseDTO.java` | Full response with reportCode, status, order/item context, patient/doctor, result fields, attachments, audit fields |
| `lab/dto/ReportAttachmentResponseDTO.java` | id, fileName, originalFileName, fileType, fileSize, fileUrl, uploadedAt, uploadedBy |
| `lab/mapper/LabOrderMapper.java` | Static class; `toResponseDTO(LabOrder, LabReportRepository)` — passes repo as parameter to populate hasReport/reportCode/reportStatus per item |
| `lab/mapper/LabReportMapper.java` | Static class; `toResponseDTO(LabReport)` — maps all fields including nested attachments; createdAt→uploadedAt, createdBy→uploadedBy |
| `lab/repository/LabOrderRepository.java` | findByLabOrderCode, findTopByLabOrderCodeStartingWith (code gen), findByPatient, findByDoctor, searchLabOrders JPQL, countByStatus |
| `lab/repository/LabOrderItemRepository.java` | findByLabOrder_LabOrderCode, existsByLabOrder_LabOrderCodeAndTestName |
| `lab/repository/LabReportRepository.java` | findByReportCode, findTopByReportCodeStartingWith (code gen), findByLabOrderItem_Id, existsByLabOrderItem_Id, searchLabReports |
| `lab/repository/ReportAttachmentRepository.java` | findByLabReport_ReportCode |
| `lab/service/LabOrderService.java` | Interface: createLabOrder, getLabOrderByCode, getAllLabOrders, getLabOrdersByPatient, getLabOrdersByDoctor, updateLabOrderStatus |
| `lab/service/LabReportService.java` | Interface: createLabReport, updateLabReport, getLabReportByCode, getAllLabReports, getLabReportsByLabOrder, verifyLabReport, uploadAttachment, getAttachments, downloadAttachment |
| `lab/service/impl/LabOrderServiceImpl.java` | Full implementation; derives patient/doctor from consultation; validates doctor ownership; checks duplicate test names (case-insensitive); status transition validation (CANCELLED blocks updates; COMPLETED → only CANCELLED allowed) |
| `lab/service/impl/LabReportServiceImpl.java` | Full implementation; `@Value("${app.upload.directory}")` for upload path; MAX_FILE_SIZE=10MB; file validation (MIME + extension blacklist); auto-promotes PENDING→READY when resultValue added or file uploaded; PATIENT can only download VERIFIED reports |
| `lab/controller/LabOrderController.java` | Base path `/api/v1/lab-orders`; POST create, GET list/search, GET by code, GET by patient, GET by doctor, PUT status update |
| `lab/controller/LabReportController.java` | Base path `/api/v1/lab-reports`; POST create, PUT update, GET list, GET by code, GET by order, PUT verify, POST upload attachment, GET list attachments, GET download attachment |

#### Modified Backend Files (2)

| File | Change |
|---|---|
| `config/SecurityConfig.java` | Added before consultation rules: `POST /api/v1/lab-orders → hasAnyRole("ADMIN","DOCTOR")`, `PUT /api/v1/lab-orders/** → hasAnyRole("ADMIN","DOCTOR")`, `POST /api/v1/lab-reports → hasAnyRole("ADMIN","DOCTOR")`, `PUT /api/v1/lab-reports/** → hasAnyRole("ADMIN","DOCTOR")` |
| `src/main/resources/application.yaml` | Added `spring.servlet.multipart` config (enabled, max-file-size 10MB, max-request-size 11MB); added `app.upload.directory: uploads/reports` sibling to `app.jwt` under `app:` |

#### New Frontend Files (2)

| File | Purpose |
|---|---|
| `hspui/src/pages/Reports/LabReports.jsx` | Main lab reports list page; stat cards (Total Orders, Pending, In Progress, Completed); search by order/patient/doctor code; filter tabs (ALL/ORDERED/SAMPLE_COLLECTED/IN_PROGRESS/COMPLETED/CANCELLED); paginated table (code, patient, doctor, tests count badge, priority badge, status badge, date); click → navigate to detail page |
| `hspui/src/pages/Reports/LabReportDetail.jsx` | Lab order detail workspace; breadcrumb; header with code + status + priority + progress bar; next-action button (Mark Sample Collected / In Progress / Completed); Cancel Order button; patient + doctor info cards; order notes; tests section with per-test: report status badge, ResultEditor (inline create/update form), file upload section, AttachmentRow component (download via fetch+blob+anchor); VERIFIED reports locked |

#### Modified Frontend Files (2)

| File | Change |
|---|---|
| `hspui/src/App.jsx` | Added `import LabReports` and `import LabReportDetail`; added `<Route path="reports" element={<LabReports />} />` and `<Route path="reports/:labOrderCode" element={<LabReportDetail />} />` inside protected layout |
| `hspui/src/pages/Consultations/ConsultationWorkspace.jsx` | Added `FlaskConical` to lucide-react import; added lab order modal state (7 state vars); added `addLabItem`, `removeLabItem`, `updateLabItem`, `resetLabModal`, `handleOrderLabTests` helpers; added "Order Lab Tests" button in action bar (between Save Draft and Complete Consultation); added `AnimatePresence` lab order modal with: priority selector (NORMAL/URGENT/STAT), test rows table (add/remove with testName/testCode/category), clinical notes + instructions textareas, error/success banners, Place Order button |

#### API Endpoints

| Method | Path | Role | Action |
|---|---|---|---|
| `POST` | `/api/v1/lab-orders` | ADMIN, DOCTOR | Create lab order for a consultation |
| `GET` | `/api/v1/lab-orders?search=&page=&size=` | ANY AUTHENTICATED | List/search lab orders |
| `GET` | `/api/v1/lab-orders/{labOrderCode}` | ANY AUTHENTICATED | Get lab order (ownership enforced in service) |
| `GET` | `/api/v1/lab-orders/patient/{patientCode}` | ANY AUTHENTICATED | List by patient |
| `GET` | `/api/v1/lab-orders/doctor/{doctorCode}` | ANY AUTHENTICATED | List by doctor |
| `PUT` | `/api/v1/lab-orders/{labOrderCode}/status` | ADMIN, DOCTOR | Update lab order status |
| `POST` | `/api/v1/lab-reports` | ADMIN, DOCTOR | Create lab report for an order item |
| `PUT` | `/api/v1/lab-reports/{reportCode}` | ADMIN, DOCTOR | Update report results |
| `GET` | `/api/v1/lab-reports?search=&page=&size=` | ANY AUTHENTICATED | List/search reports |
| `GET` | `/api/v1/lab-reports/{reportCode}` | ANY AUTHENTICATED | Get report by code |
| `GET` | `/api/v1/lab-reports/order/{labOrderCode}` | ANY AUTHENTICATED | List reports for an order |
| `PUT` | `/api/v1/lab-reports/{reportCode}/verify` | ADMIN, DOCTOR | Verify a READY report |
| `POST` | `/api/v1/lab-reports/{reportCode}/attachments` | ADMIN, DOCTOR | Upload file attachment (multipart) |
| `GET` | `/api/v1/lab-reports/{reportCode}/attachments` | ANY AUTHENTICATED | List attachments |
| `GET` | `/api/v1/lab-reports/attachments/{attachmentId}/download` | ANY AUTHENTICATED | Download file (streamed Resource) |

#### Security Model

| Role | Lab Orders (write) | Lab Reports (write) | Read | Download |
|---|---|---|---|---|
| ADMIN | All | All | All | All |
| DOCTOR | Own consultation orders only | Own orders only | Own orders/reports | All reports |
| PATIENT | Denied (403 URL level) | Denied (403 URL level) | Own VERIFIED reports only | VERIFIED own reports only |

#### DB Tables Added by Hibernate (ddl-auto: update)

| Table | Key Columns |
|---|---|
| `lab_orders` | `id`, `lab_order_code` (unique), `consultation_id` (FK), `appointment_id` (FK), `patient_id` (FK), `doctor_id` (FK), `order_date`, `priority`, `status`, `clinical_notes`, `instructions`, audit columns |
| `lab_order_items` | `id`, `lab_order_id` (FK), `test_code`, `test_name`, `category`, `remarks`, audit columns |
| `lab_reports` | `id`, `report_code` (unique), `lab_order_id` (FK), `lab_order_item_id` (FK, unique with lab_order_id), `patient_id` (FK), `doctor_id` (FK), `result_value`, `reference_range`, `abnormal_flag`, `interpretation`, `remarks`, `report_status`, audit columns |
| `report_attachments` | `id`, `lab_report_id` (FK), `file_name`, `original_file_name`, `file_type`, `file_size`, `file_url`, audit columns |

#### Edge Cases Handled

| Edge Case | Handling |
|---|---|
| Patient/doctor not from consultation payload | Derived from `consultation.patient` and `consultation.doctor` — never from request body |
| Duplicate test in same lab order | Case-insensitive HashSet check before save; 422 with test name in message |
| Duplicate report for same test | `existsByLabOrderItem_Id()` check + DB unique constraint on `(lab_order_id, lab_order_item_id)` |
| CANCELLED lab order status update | 422 — cancelled orders cannot be updated |
| Report editing after VERIFIED | 422 — verified reports are immutable |
| Verify PENDING report (no results yet) | 422 — must be READY (has results or attachment) before verification |
| File > 10 MB | `InvalidReportFileException` → 422 |
| Disallowed MIME type | `InvalidReportFileException` → 422 (PDF, PNG, JPG/JPEG only) |
| Patient downloading unverified report | 403 `AccessDeniedException` |
| Doctor accessing another doctor's order | 403 `AccessDeniedException` |
| Frontend multipart upload | Raw `fetch` with `FormData` — apiService JSON wrapper explicitly NOT used (sets Content-Type: application/json which breaks multipart boundary) |
| Frontend PDF/image download | Raw `fetch` with `Authorization: Bearer` header → blob → anchor click (window.open has no auth header) |
| Lab orders allowed on COMPLETED consultations | Explicitly allowed — post-consultation lab tests are a valid clinical workflow |

#### Workflow Integration

```
Consultation DRAFT or COMPLETED
  → ConsultationWorkspace: "Order Lab Tests" button → modal
  → POST /api/v1/lab-orders { consultationCode, priority, items }
  → LabOrder created (status = ORDERED)
  → Lab technician: PUT /lab-orders/{code}/status → SAMPLE_COLLECTED → IN_PROGRESS
  → POST /api/v1/lab-reports { labOrderCode, labOrderItemId, resultValue… }
  → POST /api/v1/lab-reports/{code}/attachments (multipart file upload)
  → Report auto-promotes PENDING → READY
  → Doctor: PUT /lab-reports/{code}/verify → VERIFIED
  → Patient: GET /lab-reports/attachments/{id}/download (only for VERIFIED)
```

---

### [#12] Consultation Module — Enterprise Clinical Encounter Workflow — 2026-05-20

**Type:** New feature — full-stack consultation module (backend + frontend)

**Summary:** Implemented the core clinical consultation workflow as an extension of the appointment lifecycle. Doctors can start a consultation from an IN_PROGRESS appointment, record vitals, clinical notes, and structured prescriptions in a DRAFT state, and then complete the consultation to lock the clinical record and automatically transition the appointment to COMPLETED. Patients can view their completed consultation history in read-only mode.

#### Design Contract

- **Consultation is NOT standalone CRUD.** It is an extension of the appointment workflow.
- **One appointment → one consultation** (enforced by unique constraint on `appointment_id`).
- **One consultation → many prescription items** (structured rows; never a text blob).
- **Patient/doctor derived from appointment** — never manually entered in the consultation form.
- **DRAFT → COMPLETED** lifecycle: doctors may partially save, continue later, and finalise. Once COMPLETED, the record is locked.
- **Appointment sync:** completing a consultation automatically sets appointment status → COMPLETED (via `AppointmentRepository` injection; no circular dependency with `AppointmentService`).
- **Snapshots:** `patientNameSnapshot` and `doctorNameSnapshot` frozen from appointment at consultation creation time.
- **BMI auto-calculation:** if height + weight are supplied and BMI is blank, service computes BMI = weight / (height_m)².

#### Consultation Code Format

- Pattern: `CONS-YYYY-NNNN` (e.g., `CONS-2026-0001`)
- Year-scoped sequential, identical strategy to `APT-YYYY-NNNN`, `BILL-YYYY-NNNN`
- DB unique constraint is the concurrency safety net for single-instance deployments

#### New Backend Files (15)

| File | Purpose |
|---|---|
| `consultation/enums/ConsultationStatus.java` | `DRAFT, COMPLETED` |
| `consultation/entity/Consultation.java` | JPA entity, table `consultations`; OneToOne appointment (unique), ManyToOne patient + doctor; vitals, clinical, follow-up, status, OneToMany prescription items; extends `BaseAuditEntity` |
| `consultation/entity/PrescriptionItem.java` | JPA entity, table `prescription_items`; ManyToOne consultation; medicineName, dosage, frequency, duration, instructions; extends `BaseAuditEntity` |
| `consultation/exception/ConsultationNotFoundException.java` | Extends `ResourceNotFoundException` → 404 |
| `consultation/exception/ConsultationAlreadyExistsException.java` | Extends `ResourceAlreadyExistsException` → 409; includes existing consultation code in message for frontend navigation |
| `consultation/repository/ConsultationRepository.java` | JPA repo: findByConsultationCode, findByAppointment_AppointmentCode, existsByAppointment_AppointmentCode, findByPatient_PatientCode, findByDoctor_DoctorCode, findTopByConsultationCodeStartingWith (code gen) |
| `consultation/dto/PrescriptionItemRequestDTO.java` | `@NotBlank` on medicineName, dosage, frequency, duration; `@Size` limits |
| `consultation/dto/PrescriptionItemResponseDTO.java` | Response DTO: id, medicineName, dosage, frequency, duration, instructions |
| `consultation/dto/ConsultationSaveDTO.java` | Request DTO for both draft + complete; vital range validations (`@Min`/`@Max`); clinical fields with `@Size`; `@Future` on followUpDate; `@Valid` on prescription item list |
| `consultation/dto/ConsultationResponseDTO.java` | Full response DTO: consultationCode, appointment context, patient/doctor, vitals, clinical, follow-up, status, prescriptions, audit fields |
| `consultation/mapper/ConsultationMapper.java` | Static: `toResponseDTO()` maps all fields including nested prescriptions and computed patient age |
| `consultation/service/ConsultationService.java` | Interface: `startConsultation`, `saveDraft`, `completeConsultation`, `getConsultationByCode`, `getConsultationsByPatient`, `getConsultationsByDoctor` |
| `consultation/service/impl/ConsultationServiceImpl.java` | Full implementation with ownership validation, partial-update field mapping, prescription replacement, BMI auto-calc, follow-up validation, appointment sync on completion |
| `consultation/controller/ConsultationController.java` | REST controller, base path `/api/v1/consultations`; 6 endpoints |

#### Modified Backend Files (2)

| File | Change |
|---|---|
| `config/SecurityConfig.java` | Added `POST /api/v1/consultations/**` → `hasAnyRole("ADMIN", "DOCTOR")` and `PUT /api/v1/consultations/**` → `hasAnyRole("ADMIN", "DOCTOR")` — placed before catch-all GET rule; PATIENT explicitly excluded from write operations |
| `doctor/repository/DoctorRepository.java` | Added `Optional<Doctor> findByUser_Email(String email)` — used by `ConsultationServiceImpl.validateDoctorReadAccess()` to resolve the logged-in doctor from JWT email |

#### New Frontend Files (2)

| File | Purpose |
|---|---|
| `hspui/src/pages/Consultations/ConsultationList.jsx` | Consultation overview page; stat strip (total/draft/completed); status filter tabs; paginated consultation cards with diagnosis/chief-complaint callouts; search by CONS-, PAT-, DOC- codes; role-aware endpoint selection (admin/doctor/patient scope) |
| `hspui/src/pages/Consultations/ConsultationWorkspace.jsx` | Clinical encounter workspace; patient context bar (read-only); horizontal vitals strip (BP, Pulse, Temp, SpO2, RR, Height, Weight, BMI); clinical fields (chief complaint, symptoms, diagnosis highlighted, notes, treatment plan); dynamic prescription table (add/remove medicine rows with inline editing); follow-up toggle + date + notes; Save Draft + Complete Consultation with confirmation dialog; locked read-only view for COMPLETED consultations |

#### Modified Frontend Files (3)

| File | Change |
|---|---|
| `hspui/src/App.jsx` | Added `import ConsultationList` and `import ConsultationWorkspace`; added `<Route path="consultations" …>` and `<Route path="consultations/:consultationCode" …>` inside protected layout |
| `hspui/src/components/layout/Sidebar.jsx` | Added `ClipboardList` to lucide-react imports; added `{ path: '/consultations', label: 'Consultations', icon: ClipboardList }` to `NAV_MAIN` between Appointments and Billing |
| `hspui/src/pages/Appointments.jsx` | Added `ClipboardList` import; added `canOpenConsultation` flag for `IN_PROGRESS` appointments; added "Open Consultation" action button; added `handleOpenConsultation` handler (calls `POST /api/v1/consultations/start/{appointmentCode}`, navigates to workspace; handles 409 by extracting existing consultation code from error message) |

#### API Endpoints

| Method | Path | Role | Action |
|---|---|---|---|
| `POST` | `/api/v1/consultations/start/{appointmentCode}` | ADMIN, DOCTOR | Create DRAFT consultation for an IN_PROGRESS appointment |
| `PUT` | `/api/v1/consultations/{consultationCode}/draft` | ADMIN, DOCTOR | Save partial clinical data without completing |
| `PUT` | `/api/v1/consultations/{consultationCode}/complete` | ADMIN, DOCTOR | Complete consultation; auto-completes appointment |
| `GET` | `/api/v1/consultations/{consultationCode}` | ANY AUTHENTICATED | Get consultation (ownership enforced in service) |
| `GET` | `/api/v1/consultations/patient/{patientCode}` | ANY AUTHENTICATED | List by patient (ownership enforced in service) |
| `GET` | `/api/v1/consultations/doctor/{doctorCode}` | ANY AUTHENTICATED | List by doctor (ownership enforced in service) |

#### Security Model

| Role | Write (start/draft/complete) | Read |
|---|---|---|
| ADMIN | All consultations | All consultations |
| DOCTOR | Own appointments only (ownership validated via `appointment.doctor.user.email == JWT email`) | Own consultations only |
| PATIENT | Denied (403 at URL level) | Own consultations only (validated via `patient.user.email == JWT email`) |

#### DB Tables Added by Hibernate (ddl-auto: update)

| Table | Key Columns |
|---|---|
| `consultations` | `id`, `consultation_code` (unique), `appointment_id` (unique FK), `patient_id` (FK), `doctor_id` (FK), vitals columns, clinical TEXT columns, follow-up columns, `consultation_status`, `patient_name_snapshot`, `doctor_name_snapshot`, audit columns |
| `prescription_items` | `id`, `consultation_id` (FK), `medicine_name`, `dosage`, `frequency`, `duration`, `instructions`, audit columns |

#### Edge Cases Handled

| Edge Case | Handling |
|---|---|
| Consultation attempted before appointment IN_PROGRESS | 422 with descriptive message about CONFIRMED → IN_PROGRESS requirement |
| Consultation already exists for appointment | 409; consultation code embedded in error message; frontend extracts code and navigates directly |
| Doctor accessing another doctor's consultation | 403 AccessDeniedException; warning logged |
| Patient accessing another patient's consultation | 403 AccessDeniedException; warning logged |
| Patient modifying consultation | 403 at URL level (POST/PUT restricted to ADMIN, DOCTOR) |
| Editing COMPLETED consultation | 422 BusinessRuleViolationException; edit locked |
| Appointment cancelled during consultation | Detected on `completeConsultation()`; 422 thrown |
| followUpRequired=true but no followUpDate | 422 on complete; draft allows partial save |
| Prescription item with blank medicineName | 422 on complete; draft allows partial items |
| BMI not supplied but height + weight present | Auto-calculated in service (weight / height_m²) |
| Page refresh during draft editing | Draft is persisted via "Save Draft"; form re-hydrates on load |

#### Workflow Integration (complete lifecycle)

```
Appointment booked (PAYMENT_PENDING)
  → Bill paid → CONFIRMED
  → PUT /appointments/{code}/start → IN_PROGRESS
  → POST /consultations/start/{appointmentCode} → Consultation DRAFT created
  → PUT /consultations/{code}/draft (save partial work, repeat as needed)
  → PUT /consultations/{code}/complete
      → Consultation COMPLETED (locked)
      → Appointment COMPLETED (auto-synced)
```

---

### [#11] Invoice / Receipt PDF Module — Enterprise Invoice Workflow — 2026-05-19

**Type:** New feature — full-stack invoice PDF module (backend + frontend)

**Summary:** Implemented a complete enterprise-grade invoice PDF generation system. Invoices are dynamically generated in-memory from Bill snapshot data and HospitalSettings — no InvoiceEntity is created. The Bill entity remains the financial source of truth. PDFs are rendered with OpenPDF and streamed directly to the client (never stored on disk or in the database).

#### Design Contract

- **No InvoiceEntity** — invoices are dynamically generated from existing Bill + HospitalSettings data.
- **Invoice number format:** `INV-YYYY-NNNN` — derived from bill code by replacing `BILL-` with `INV-`. 1-to-1 mapping with bills, no separate counter.
- **Snapshot fidelity:** patient/doctor names come from `Bill.patientNameSnapshot` / `Bill.doctorNameSnapshot` (immutable from booking time). Doctor specialization/department are loaded from the live Doctor entity (no snapshot exists for them).
- **Organization config:** ALL organization details (hospital name, address, GST, currency, timezone, logo) are loaded from `HospitalSettingsService.getSettings()` — nothing is hardcoded.
- **Access control:** ADMIN — all invoices; PATIENT — own bills only (JWT email validation, same pattern as BillServiceImpl); DOCTOR — 403.
- **Business rule:** only PAID bills produce a PDF. PENDING/CANCELLED throw `InvoiceNotAvailableException` (422).
- **PDF generation:** in-memory using OpenPDF 1.3.43; returned as `application/pdf` bytes; never persisted.

#### New Dependencies (1)

| Dependency | Version | Purpose |
|---|---|---|
| `com.github.librepdf:openpdf` | `1.3.43` | Lightweight PDF generation library |

#### New Backend Files (7)

| File | Purpose |
|---|---|
| `billing/invoice/exception/InvoiceNotAvailableException.java` | Extends `BusinessRuleViolationException` → 422; thrown for non-PAID bills |
| `billing/invoice/dto/InvoiceReceiptDTO.java` | Full invoice snapshot DTO — org config + patient + doctor + appointment + payment + audit fields |
| `billing/invoice/pdf/InvoicePdfService.java` | `@Component`; pure PDF generation from `InvoiceReceiptDTO` using OpenPDF; sections: header, invoice metadata, patient/doctor, consultation, payment, footer |
| `billing/invoice/service/InvoiceService.java` | Interface: `byte[] generateInvoicePdf(String billCode)` |
| `billing/invoice/service/impl/InvoiceServiceImpl.java` | `@Transactional(readOnly=true)`; validates access + business rules; builds DTO from bill + settings; delegates to `InvoicePdfService`; logs access |
| `billing/invoice/controller/InvoiceController.java` | Two endpoints; differs only in `Content-Disposition: inline` vs `attachment` |
| (package directories) | `billing/invoice/controller/`, `dto/`, `exception/`, `pdf/`, `service/`, `service/impl/` |

#### Modified Backend Files (2)

| File | Change |
|---|---|
| `pom.xml` | Added `com.github.librepdf:openpdf:1.3.43` dependency |
| `config/SecurityConfig.java` | Added `GET /api/v1/invoices/**` rule: `hasAnyRole("ADMIN", "PATIENT")` — placed BEFORE the catch-all GET rule; explicitly blocks DOCTOR role |

#### New Frontend Files (1)

| File | Purpose |
|---|---|
| `hspui/src/pages/Invoices.jsx` | First-class invoice page; paginated invoice table; search by invoice # (INV-…), bill code (BILL-…), or patient code (PAT-…); status filter tabs (ALL/PAID/PENDING/CANCELLED); Preview + Download action buttons for PAID bills; PDF fetched via `fetchPdfBlob()` helper (includes JWT header); toast notifications for success/error; stat strip (total, paid, pending); responsive; light/dark mode via CSS custom property aliases |

#### Modified Frontend Files (3)

| File | Change |
|---|---|
| `hspui/src/App.jsx` | Added `import Invoices`; added `<Route path="invoices" element={<Invoices />} />` inside protected layout |
| `hspui/src/components/layout/Sidebar.jsx` | Added `FileText` to lucide-react imports; added `{ path: '/invoices', label: 'Invoices', icon: FileText }` to `NAV_MAIN` after Billing |
| *(none)* | Sidebar and App.jsx follow existing patterns exactly |

#### API Endpoints

| Method | Path | Role | Content-Disposition | Action |
|---|---|---|---|---|
| `GET` | `/api/v1/invoices/{billCode}` | ADMIN, PATIENT | `inline` | Preview PDF in browser |
| `GET` | `/api/v1/invoices/{billCode}/download` | ADMIN, PATIENT | `attachment` | Download PDF |

#### PDF Structure (InvoicePdfService)

| Section | Contents |
|---|---|
| Hospital Header (navy band) | Hospital name, address, phone, email, GST number; "INVOICE" label on right |
| Invoice Metadata (4-col grid) | Invoice #, Bill Code, Appointment Code, Date Generated, Paid On, Payment Method |
| Patient & Doctor (2-col) | Patient: name (snapshot), code, phone; Doctor: name (snapshot), department, specialization |
| Consultation Details (3-col) | Consultation type, appointment date, time slot |
| Payment Summary (table) | Consultation fee, tax (omitted if 0), discount (omitted if 0), highlighted total row; status + method |
| Footer | Computer-generated disclaimer, audit line (generated by + date), support email |

#### Security

- URL-level rule in `SecurityConfig`: `GET /api/v1/invoices/**` → `hasAnyRole("ADMIN", "PATIENT")` (placed before catch-all `GET /api/v1/**`)
- Service-layer ownership validation in `InvoiceServiceImpl.validateInvoiceAccess()` mirrors `BillServiceImpl.validateBillOwnership()` exactly
- DOCTOR role → 403 at both URL level and service layer (defense-in-depth)
- JWT email extracted from `UserPrincipal`; never trusts request parameters
- PDF download uses JWT Bearer header (not `window.open`, which sends no auth)

#### Frontend PDF Blob Pattern

```javascript
// Used for BOTH preview and download — avoids window.open auth limitation
const fetchPdfBlob = async (endpoint) => {
  const token = localStorage.getItem('mediflow_token');
  const resp = await fetch(endpoint, { headers: { Authorization: `Bearer ${token}` } });
  if (!resp.ok) throw error;
  return resp.blob();
};

// Preview: open blob URL in new tab
const url = URL.createObjectURL(blob);
window.open(url, '_blank', 'noopener');
setTimeout(() => URL.revokeObjectURL(url), 60000);

// Download: programmatic anchor click
anchor.href = url; anchor.download = 'Invoice_INV-2026-0001.pdf'; anchor.click();
URL.revokeObjectURL(url);
```

#### Invoice Number Derivation

```
BILL-2026-0001  →  INV-2026-0001    (replace "BILL-" prefix with "INV-")
BILL-2026-0042  →  INV-2026-0042
```

No separate counter — invoice number is always deterministically derived from the bill code.

#### Edge Cases Handled

| Edge Case | Handling |
|---|---|
| Bill not PAID | `InvoiceNotAvailableException` → 422 with descriptive message |
| Bill CANCELLED | `InvoiceNotAvailableException` → 422 |
| Bill not found | `BillNotFoundException` → 404 |
| Patient accessing another's invoice | `AccessDeniedException` → 403 |
| DOCTOR role access | 403 at URL + service layer |
| HospitalSettings missing | `BusinessRuleViolationException` → 422 with instructions |
| PDF generation failure | `RuntimeException` → 500 with log |
| Concurrent PDF requests | Stateless; each request independent; no shared state |
| Lazy association access | All within `@Transactional(readOnly=true)` — session open throughout |

---

### [#10] Hospital Settings Module — Centralized Organization Configuration — 2026-05-19

**Type:** New feature — full-stack settings module (backend + frontend)

**Summary:** Implemented the Hospital Settings module as the centralized, single-record organization configuration store. This module is the authoritative source of truth for hospital identity, contact, address, financial, and branding data. It is the prerequisite for the upcoming Invoice / Receipt PDF module — all invoice branding and org data must be read from `HospitalSettings`, never hardcoded.

#### Design Contract

- `hospital_settings` table contains **exactly ONE row** at all times.
- No "create new hospital" or DELETE endpoint is exposed publicly.
- `DataInitializer` seeds the default row at startup (idempotent).
- `updateSettings()` uses a find-or-create pattern: it updates the existing row or creates one if missing (safety net only — DataInitializer prevents the missing-row case).
- `@DynamicUpdate` on the entity ensures Hibernate only sends changed columns in UPDATE statements.

#### New Backend Files (8)

| File | Purpose |
|---|---|
| `settings/entity/HospitalSettings.java` | JPA entity, table `hospital_settings`; `@DynamicUpdate`; extends `BaseAuditEntity`; `@UniqueConstraint` on `hospital_code`; `@Builder.Default` for `currencyCode=INR`, `timezone=Asia/Kolkata` |
| `settings/dto/HospitalSettingsRequestDTO.java` | Update request DTO; `@NotBlank` on hospitalName, hospitalCode, currencyCode, timezone; `@Pattern` for phone, hospitalCode, GST; `@Email` for email fields; `@Size` limits on all string fields |
| `settings/dto/HospitalSettingsResponseDTO.java` | Response DTO; all fields + audit fields (`createdAt`, `updatedAt`, `createdBy`, `updatedBy`) with `@JsonFormat` |
| `settings/mapper/HospitalSettingsMapper.java` | Static mapper: `toResponseDTO()`, `updateEntityFromDTO()` — `updateEntityFromDTO` normalizes blank strings to null for optional fields |
| `settings/repository/HospitalSettingsRepository.java` | JPA repo; single query: `findTopByOrderByIdAsc()` — returns the one settings row |
| `settings/service/HospitalSettingsService.java` | Interface: `getSettings()`, `updateSettings(request)` |
| `settings/service/impl/HospitalSettingsServiceImpl.java` | Implementation; `getSettings()` is `@Transactional(readOnly=true)` — returns in-memory default if row missing; `updateSettings()` validates timezone via `ZoneId.of()` before persisting; throws `BusinessRuleViolationException` (422) for invalid timezone |
| `settings/controller/HospitalSettingsController.java` | REST controller, base path `/api/v1/settings/hospital`; `@PreAuthorize("hasRole('ADMIN')")` on both endpoints; `GET` + `PUT` only — no POST, no DELETE |

#### Modified Backend Files (2)

| File | Change |
|---|---|
| `config/SecurityConfig.java` | Added URL-level ADMIN rules for `GET /api/v1/settings/**` and `PUT /api/v1/settings/**` — placed before the catch-all `GET /api/v1/**` authenticated rule; provides defense-in-depth alongside `@PreAuthorize` |
| `auth/config/DataInitializer.java` | Added `HospitalSettingsRepository` injection; added `seedHospitalSettings()` called from `run()` — idempotent, skipped if row already exists; seeds `hospitalCode=MEDIFLOW`, `hospitalName=MediFlow Hospital`, `currencyCode=INR`, `timezone=Asia/Kolkata` |

#### New Frontend Files (1)

| File | Purpose |
|---|---|
| `hspui/src/pages/Settings/HospitalSettings.jsx` | Enterprise-grade settings form; 5 sections: Organization Identity, Contact Information, Address, Financial & Legal, Branding; animated loading skeleton; success/error banners with `AnimatePresence`; field-level validation error display; "Last updated by/on" badge; logo preview with error-image guard; save button with spinner; all theming via CSS custom property aliases (`bg-card`, `bg-surface`, `text-tx1`, etc.) — no `dark:` Tailwind variants |

#### Modified Frontend Files (1)

| File | Change |
|---|---|
| `hspui/src/App.jsx` | Added `import HospitalSettings` from `./pages/Settings/HospitalSettings`; added `<Route path="settings" element={<HospitalSettings />} />` inside the protected layout routes |

#### API Endpoints

| Method | Path | Role | Action |
|---|---|---|---|
| `GET` | `/api/v1/settings/hospital` | ADMIN | Fetch current organization settings |
| `PUT` | `/api/v1/settings/hospital` | ADMIN | Update organization settings |

#### DB Table Added by Hibernate (ddl-auto: update)

| Table | Key Columns |
|---|---|
| `hospital_settings` | `id`, `hospital_code` (unique), `hospital_name`, `phone_number`, `alternate_phone_number`, `email`, `support_email`, `website`, `support_phone`, `address_line1`, `address_line2`, `city`, `state`, `postal_code`, `country`, `logo_url`, `gst_number`, `currency_code`, `timezone`, `created_at`, `updated_at`, `created_by`, `updated_by` |

#### Security

- DOCTOR role → 403 Forbidden on both GET and PUT (enforced at URL level + @PreAuthorize)
- PATIENT role → 403 Forbidden on both GET and PUT
- Unauthenticated → 401 Unauthorized

#### Future Integration Points (NOT implemented here)

The following modules must read from `HospitalSettingsService.getSettings()` — never hardcode these values:

| Future Module | Fields to Consume |
|---|---|
| Invoice / Receipt PDF | hospitalName, address fields, gstNumber, currencyCode, logoUrl |
| Email notifications | email, supportEmail, hospitalName |
| Report headers | hospitalName, hospitalCode, timezone |
| Export footers | website, phoneNumber, address fields |

---

### [#9] Billing + Appointment Workflow Hardening — 2026-05-13

**Type:** Feature + Security hardening — closes 5 operational gaps in the billing/appointment workflow

**Summary:** Completed the enterprise consultation lifecycle with payment timeout auto-cancellation, the missing CONFIRMED→IN_PROGRESS start endpoint, patient-level bill ownership security, immutable bill name snapshots, and improved cancellation rules that handle the CONFIRMED (already-paid) path correctly.

---

#### GAP 1 — Payment Timeout Auto-Cancellation

**Problem:** PAYMENT_PENDING appointments could block doctor slots indefinitely if payment was never completed.

**Solution:** Spring `@Scheduled` task scans every 60 seconds for PAYMENT_PENDING appointments older than 15 minutes and cancels them + their linked bills transactionally.

**Files changed:**

| File | Change |
|---|---|
| `MediflowPlatformApplication.java` | Added `@EnableScheduling` |
| `appointment/repository/AppointmentRepository.java` | Added `findByAppointmentStatusAndCreatedAtBefore(AppointmentStatus, LocalDateTime)` derived query method |
| `billing/service/BillService.java` | Added `expirePaymentPendingAppointment(Long appointmentId)` to interface |
| `billing/service/impl/BillServiceImpl.java` | Implemented `expirePaymentPendingAppointment()` — re-validates status inside `@Transactional` to guard against the race condition where payment arrives between scheduler scan and execution; cancels appointment + bill atomically |
| `billing/scheduler/PaymentTimeoutScheduler.java` | **NEW** — `@Component`; `@Scheduled(fixedDelay=60_000)`; queries expired PAYMENT_PENDING appointments; processes each through `billService.expirePaymentPendingAppointment()` with per-appointment error isolation |

**Timeout:** 15 minutes from `createdAt`.
**Race condition guard:** Status is re-checked inside the `@Transactional` method — if the appointment was paid between the scan and execution, the expiry is a silent no-op.

---

#### GAP 2 — Start Consultation Workflow

**Problem:** No API existed to transition CONFIRMED → IN_PROGRESS.

**Solution:** New `PUT /api/v1/appointments/{appointmentCode}/start` endpoint.

**Business rules:**
- Only `CONFIRMED` appointments can be started (payment must be received first)
- `PAYMENT_PENDING`, `CANCELLED`, `COMPLETED`, `NO_SHOW` are rejected with 422

| File | Change |
|---|---|
| `appointment/service/AppointmentService.java` | Added `startConsultation(String appointmentCode)` to interface |
| `appointment/service/impl/AppointmentServiceImpl.java` | Implemented `startConsultation()` — validates CONFIRMED status, transitions to IN_PROGRESS |
| `appointment/controller/AppointmentController.java` | Added `PUT /{appointmentCode}/start` with Swagger docs; also updated `/cancel` description for the new GAP 5 rules |

---

#### GAP 3 — Bill Ownership Validation

**Problem:** Patients could access bills belonging to other patients.

**Solution:** `validateBillOwnership(String patientCode)` private helper in `BillServiceImpl`, called before every read operation.

**Access matrix:**

| Role | Access |
|---|---|
| ADMIN | Full access to all bills |
| PATIENT | Own bills only (validated via JWT email ↔ Patient.user.email) |
| DOCTOR | Denied (403) |
| Anonymous | Denied (403) |

**Implementation detail:** The logged-in email is extracted from `UserPrincipal.getEmail()` (JWT SecurityContext). The patient's email comes from `patient.getUser().getEmail()`. If `patient.getUser()` is null (legacy record), access is denied. Throws Spring `AccessDeniedException` → existing `GlobalExceptionHandler` maps it to HTTP 403.

| File | Change |
|---|---|
| `billing/service/impl/BillServiceImpl.java` | Added `PatientRepository` injection; added `validateBillOwnership()` private helper; called from `getBillByCode()` and `getBillsByPatient()` |

**Logging:** Warning logged on any forbidden access attempt with the caller's email and the target patient code.

---

#### GAP 4 — Immutable Bill Name Snapshots

**Problem:** `BillMapper` read patient/doctor names from live entities — name changes would silently alter historical invoices.

**Solution:** Two new snapshot columns on the `bills` table, frozen at bill generation time.

| File | Change |
|---|---|
| `billing/entity/Bill.java` | Added `patientNameSnapshot (VARCHAR 255, NOT NULL)` and `doctorNameSnapshot (VARCHAR 255, NOT NULL)` |
| `billing/service/impl/BillServiceImpl.java` | `generateBillForAppointment()` now sets `.patientNameSnapshot(appointment.getPatientNameSnapshot())` and `.doctorNameSnapshot(appointment.getDoctorNameSnapshot())` — values already frozen at booking time |
| `billing/mapper/BillMapper.java` | `toResponseDTO()` now reads `bill.getPatientNameSnapshot()` and `bill.getDoctorNameSnapshot()` instead of `patient.getFirstName()` etc. — DTO field names unchanged (backward-compatible) |

**DB schema change (ddl-auto: update):** Columns `patient_name_snapshot` and `doctor_name_snapshot` added to `bills` table.

---

#### GAP 5 — Improved Cancellation Rules

**Problem:** Only `PAYMENT_PENDING` appointments could be cancelled — too restrictive for real hospital workflows.

**New rules:**

| Previous Status | Cancel Allowed? | Bill outcome |
|---|---|---|
| `PAYMENT_PENDING` | Yes | Bill → `CANCELLED` |
| `CONFIRMED` | Yes | Bill remains `PAID` (refund = future phase) |
| `IN_PROGRESS` | No — 422 | — |
| `COMPLETED` | No — 422 | — |
| `NO_SHOW` | No — 422 | — |

| File | Change |
|---|---|
| `appointment/service/impl/AppointmentServiceImpl.java` | `cancelAppointment()` now accepts `PAYMENT_PENDING` OR `CONFIRMED`; only calls `billService.cancelBillForAppointment()` when `previousStatus == PAYMENT_PENDING`; logs `previousStatus` for audit trail |
| `appointment/controller/AppointmentController.java` | Updated Swagger `@Operation` description to document new cancellation rules |

---

#### Updated Full State Machine

```
PAYMENT_PENDING  →  CANCELLED    (via /cancel; bill also CANCELLED)
PAYMENT_PENDING  →  CONFIRMED    (via /bills/{code}/pay; bill PAID)
PAYMENT_PENDING  →  CANCELLED    (auto, via PaymentTimeoutScheduler after 15 min; bill CANCELLED)
CONFIRMED        →  IN_PROGRESS  (via /appointments/{code}/start)
CONFIRMED        →  CANCELLED    (via /cancel; bill remains PAID)
IN_PROGRESS      →  COMPLETED    (via /appointments/{code}/complete)
```

---

### [#8] Billing Module — Consultation Billing Lifecycle — 2026-05-13

**Type:** New feature — complete consultation billing module + appointment status refactor

**Summary:** Implemented the Billing Module as the financial lifecycle layer for consultation appointments. When an appointment is booked, a consultation bill is automatically generated in the same transaction. The appointment remains in PAYMENT_PENDING status until the receptionist manually marks the bill as PAID, at which point the appointment transitions to CONFIRMED. Cancelling a PAYMENT_PENDING appointment also cancels its associated bill in the same transaction.

#### AppointmentStatus Refactor

Replaced `SCHEDULED` with two semantically distinct statuses:

| Old | New | Meaning |
|---|---|---|
| `SCHEDULED` | `PAYMENT_PENDING` | Appointment reserved; consultation fee not yet paid |
| _(new)_ | `CONFIRMED` | Consultation fee paid; appointment confirmed |

Full new status set: `PAYMENT_PENDING`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `NO_SHOW`

#### State Machine (updated)

```
PAYMENT_PENDING  →  CANCELLED   (via /appointments/{code}/cancel — also cancels bill)
PAYMENT_PENDING  →  CONFIRMED   (via /bills/{code}/pay — payment recorded)
CONFIRMED        →  IN_PROGRESS (future: /appointments/{code}/start — not yet implemented)
IN_PROGRESS      →  COMPLETED   (via /appointments/{code}/complete)
```

#### New Files Created (14)

| File | Purpose |
|---|---|
| `billing/enums/PaymentStatus.java` | `PENDING, PAID, FAILED, REFUNDED, PARTIAL` |
| `billing/enums/BillStatus.java` | `GENERATED, CANCELLED, VOID` |
| `billing/enums/BillType.java` | `CONSULTATION` |
| `billing/enums/PaymentMethod.java` | `CASH, CARD, UPI, ONLINE, INSURANCE` |
| `billing/entity/Bill.java` | JPA entity, table `bills`; OneToOne with Appointment (unique FK); ManyToOne Patient + Doctor; consultationFeeSnapshot frozen at generation time; inherits BaseAuditEntity |
| `billing/exception/BillNotFoundException.java` | Extends `ResourceNotFoundException` → 404 |
| `billing/repository/BillRepository.java` | JPA repo: findByBillCode, findTopByBillCodeStartingWith (code gen), findByPatient_PatientCode (paginated), findByAppointment_AppointmentCode |
| `billing/dto/BillResponseDTO.java` | Response DTO with all bill fields + audit fields |
| `billing/dto/PayBillRequestDTO.java` | `paymentMethod` (@NotNull) — the only field needed for manual payment |
| `billing/mapper/BillMapper.java` | Static: `toResponseDTO()` — reads patient/doctor names from live entities, fee from snapshot |
| `billing/service/BillService.java` | Interface: `generateBillForAppointment`, `cancelBillForAppointment`, `getBillByCode`, `getBillsByPatient`, `payBill` |
| `billing/service/impl/BillServiceImpl.java` | Full implementation; injects BillRepository + AppointmentRepository only (no circular deps) |
| `billing/controller/BillController.java` | REST controller, base path `/api/v1/bills` |

One package-level directory created: `billing/` with sub-packages `controller/`, `dto/`, `entity/`, `enums/`, `exception/`, `mapper/`, `repository/`, `service/`, `service/impl/`

#### Existing Files Modified (7)

| File | Change |
|---|---|
| `appointment/enums/AppointmentStatus.java` | Replaced `SCHEDULED` with `PAYMENT_PENDING` + `CONFIRMED` |
| `appointment/entity/Appointment.java` | Default status: `PAYMENT_PENDING` (was `SCHEDULED`) |
| `appointment/mapper/AppointmentMapper.java` | `toEntity()` sets `PAYMENT_PENDING` (was `SCHEDULED`) |
| `appointment/service/impl/AppointmentServiceImpl.java` | (1) Added `BillService billService` field. (2) `bookAppointment()` calls `billService.generateBillForAppointment(saved)` after appointment save. (3) `cancelAppointment()` now checks for `PAYMENT_PENDING` (was `SCHEDULED`) and calls `billService.cancelBillForAppointment(code)` after save. |
| `appointment/controller/AppointmentController.java` | Updated Swagger descriptions to reflect new status names |
| `patient/service/impl/PatientServiceImpl.java` | Deactivation guard list: `[PAYMENT_PENDING, CONFIRMED, IN_PROGRESS]` (was `[SCHEDULED, IN_PROGRESS]`) |
| `doctor/service/impl/DoctorServiceImpl.java` | Same deactivation guard update as PatientServiceImpl |

#### Bill Code Format

- Pattern: `BILL-YYYY-NNNN` (e.g., `BILL-2026-0001`)
- Year-scoped sequential, identical strategy to `DOC-YYYY-NNNN` and `APT-YYYY-NNNN`
- DB unique constraint is the concurrency safety net for single-instance deployments

#### Bill Generation Flow (same transaction as appointment booking)

```
POST /api/v1/appointments
  → Validate patient, doctor, date, time, overlap
  → Save Appointment (status = PAYMENT_PENDING)
  → billService.generateBillForAppointment(appointment)
      → consultationFeeSnapshot = appointment.consultationFeeSnapshot (already frozen)
      → taxAmount = 0, discountAmount = 0, totalAmount = fee
      → paymentStatus = PENDING, billStatus = GENERATED
      → Save Bill
  → Return AppointmentResponseDTO
```

If bill save fails → entire transaction rolls back → no orphan appointment.

#### Payment Flow

```
PUT /api/v1/bills/{billCode}/pay  { "paymentMethod": "CASH" }
  → Validate bill exists
  → Validate paymentStatus != PAID
  → Validate appointment status != CANCELLED
  → bill.paymentStatus = PAID, bill.paidAt = now(), bill.paymentMethod = CASH
  → appointment.appointmentStatus = CONFIRMED
  → Save both in same transaction
  → Return BillResponseDTO
```

#### Cancellation Flow

```
PUT /api/v1/appointments/{code}/cancel
  → Validate appointment exists
  → Validate status == PAYMENT_PENDING (only cancellable before payment)
  → appointment.appointmentStatus = CANCELLED
  → billService.cancelBillForAppointment(code) → bill.billStatus = CANCELLED
  → Save both in same transaction
```

#### API Endpoints

| Method | Path | Action | Response |
|---|---|---|---|
| `GET` | `/api/v1/bills/{billCode}` | Get bill by code | 200 OK |
| `GET` | `/api/v1/bills/patient/{patientCode}` | List patient's bills (paginated, newest first) | 200 OK |
| `PUT` | `/api/v1/bills/{billCode}/pay` | Record manual payment | 200 OK |

#### Dependency Design (no circular deps)

- `AppointmentServiceImpl` → `BillService` (interface)
- `BillServiceImpl` → `BillRepository` + `AppointmentRepository` (NOT AppointmentService)
- No cycle exists.

#### DB Tables Added by Hibernate (ddl-auto: update)

| Table | Key Columns |
|---|---|
| `bills` | `id`, `bill_code` (unique), `appointment_id` (unique FK), `patient_id` (FK), `doctor_id` (FK), `bill_type`, `consultation_fee_snapshot`, `tax_amount`, `discount_amount`, `total_amount`, `payment_status`, `bill_status`, `payment_method`, `generated_at`, `paid_at`, `created_at`, `updated_at`, `created_by`, `updated_by` |

#### Future Phases (NOT implemented)

- Razorpay / Stripe payment gateway integration
- Refund workflow
- Insurance billing
- Pharmacy / lab billing
- Bill line items (multiple services per bill)

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
