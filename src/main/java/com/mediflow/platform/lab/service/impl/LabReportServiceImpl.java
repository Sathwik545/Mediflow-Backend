package com.mediflow.platform.lab.service.impl;

import com.mediflow.platform.auth.security.UserPrincipal;
import com.mediflow.platform.common.exception.BusinessRuleViolationException;
import com.mediflow.platform.lab.dto.LabReportRequestDTO;
import com.mediflow.platform.lab.dto.LabReportResponseDTO;
import com.mediflow.platform.lab.dto.LabReportUpdateDTO;
import com.mediflow.platform.lab.dto.ReportAttachmentResponseDTO;
import com.mediflow.platform.lab.entity.LabOrder;
import com.mediflow.platform.lab.entity.LabOrderItem;
import com.mediflow.platform.lab.entity.LabReport;
import com.mediflow.platform.lab.entity.ReportAttachment;
import com.mediflow.platform.lab.enums.ReportStatus;
import com.mediflow.platform.lab.exception.InvalidReportFileException;
import com.mediflow.platform.lab.exception.LabOrderNotFoundException;
import com.mediflow.platform.lab.exception.LabReportNotFoundException;
import com.mediflow.platform.lab.mapper.LabReportMapper;
import com.mediflow.platform.lab.pdf.LabReportPdfService;
import com.mediflow.platform.lab.repository.LabOrderItemRepository;
import com.mediflow.platform.lab.repository.LabOrderRepository;
import com.mediflow.platform.lab.repository.LabReportRepository;
import com.mediflow.platform.lab.repository.ReportAttachmentRepository;
import com.mediflow.platform.lab.service.LabReportService;
import com.mediflow.platform.settings.dto.HospitalSettingsResponseDTO;
import com.mediflow.platform.settings.service.HospitalSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabReportServiceImpl implements LabReportService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "application/pdf", "image/png", "image/jpeg"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".pdf", ".png", ".jpg", ".jpeg"
    );

    private final LabReportRepository labReportRepository;
    private final LabOrderRepository labOrderRepository;
    private final LabOrderItemRepository labOrderItemRepository;
    private final ReportAttachmentRepository attachmentRepository;
    private final LabReportPdfService labReportPdfService;
    private final HospitalSettingsService hospitalSettingsService;

    @Value("${app.upload.directory:uploads/reports}")
    private String uploadDirectory;

    // ── Create Lab Report ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public LabReportResponseDTO createLabReport(LabReportRequestDTO request) {
        LabOrder labOrder = labOrderRepository.findByLabOrderCode(request.getLabOrderCode())
                .orElseThrow(() -> new LabOrderNotFoundException(request.getLabOrderCode()));

        LabOrderItem labOrderItem = labOrderItemRepository.findById(request.getLabOrderItemId())
                .orElseThrow(() -> new BusinessRuleViolationException(
                    "Lab order item not found: " + request.getLabOrderItemId()));

        // Validate item belongs to the specified order
        if (!labOrderItem.getLabOrder().getId().equals(labOrder.getId())) {
            throw new BusinessRuleViolationException(
                "Lab order item " + request.getLabOrderItemId() +
                " does not belong to lab order " + request.getLabOrderCode());
        }

        // Prevent duplicate reports per item
        if (labReportRepository.existsByLabOrderItem_Id(request.getLabOrderItemId())) {
            throw new BusinessRuleViolationException(
                "A report already exists for test '" + labOrderItem.getTestName() +
                "'. Use the update endpoint to modify it.");
        }

        validateDoctorWriteAccess(labOrder);

        String reportCode = generateReportCode();

        String patientSnapshot = labOrder.getPatient().getFirstName() + " " + labOrder.getPatient().getLastName();
        String doctorSnapshot  = labOrder.getDoctor().getFirstName()  + " " + labOrder.getDoctor().getLastName();

        LabReport report = LabReport.builder()
                .reportCode(reportCode)
                .labOrder(labOrder)
                .labOrderItem(labOrderItem)
                .patient(labOrder.getPatient())
                .doctor(labOrder.getDoctor())
                .patientNameSnapshot(patientSnapshot)
                .doctorNameSnapshot(doctorSnapshot)
                .resultValue(request.getResultValue())
                .referenceRange(request.getReferenceRange())
                .abnormalFlag(request.getAbnormalFlag() != null ? request.getAbnormalFlag() : false)
                .interpretation(request.getInterpretation())
                .remarks(request.getRemarks())
                .reportStatus(hasResults(request) ? ReportStatus.READY : ReportStatus.PENDING)
                .build();

        LabReport saved = labReportRepository.save(report);

        log.info("Lab report created | code={}, order={}, test={}",
                reportCode, request.getLabOrderCode(), labOrderItem.getTestName());

        return LabReportMapper.toResponseDTO(saved);
    }

    // ── Update Lab Report ─────────────────────────────────────────────────────

    @Override
    @Transactional
    public LabReportResponseDTO updateLabReport(String reportCode, LabReportUpdateDTO request) {
        LabReport report = labReportRepository.findByReportCode(reportCode)
                .orElseThrow(() -> new LabReportNotFoundException(reportCode));

        if (report.getReportStatus() == ReportStatus.VERIFIED) {
            throw new BusinessRuleViolationException(
                "Report " + reportCode + " has been VERIFIED and cannot be modified.");
        }

        validateDoctorWriteAccess(report.getLabOrder());

        if (request.getResultValue()    != null) report.setResultValue(request.getResultValue());
        if (request.getReferenceRange() != null) report.setReferenceRange(request.getReferenceRange());
        if (request.getAbnormalFlag()   != null) report.setAbnormalFlag(request.getAbnormalFlag());
        if (request.getInterpretation() != null) report.setInterpretation(request.getInterpretation());
        if (request.getRemarks()        != null) report.setRemarks(request.getRemarks());

        // Promote to READY when result data is present
        if (report.getReportStatus() == ReportStatus.PENDING
                && StringUtils.hasText(report.getResultValue())) {
            report.setReportStatus(ReportStatus.READY);
        }

        LabReport saved = labReportRepository.save(report);
        log.info("Lab report updated | code={}", reportCode);
        return LabReportMapper.toResponseDTO(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public LabReportResponseDTO getLabReportByCode(String reportCode) {
        LabReport report = labReportRepository.findByReportCode(reportCode)
                .orElseThrow(() -> new LabReportNotFoundException(reportCode));
        validateReadAccess(report);
        return LabReportMapper.toResponseDTO(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LabReportResponseDTO> getAllLabReports(String search, int page, int size) {
        Authentication auth = getAuthentication();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String term = (search != null && !search.isBlank()) ? search.trim() : "";

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return term.isEmpty()
                ? labReportRepository.findAll(pageable).map(LabReportMapper::toResponseDTO)
                : labReportRepository.searchLabReports(term, pageable).map(LabReportMapper::toResponseDTO);
        }

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String email = principal.getEmail();

        boolean isDoctor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        if (isDoctor) {
            return labReportRepository.findByDoctorEmailAndSearch(email, term, pageable)
                    .map(LabReportMapper::toResponseDTO);
        }

        boolean isPatient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        if (isPatient) {
            return labReportRepository.findVerifiedByPatientEmailAndSearch(email, term, pageable)
                    .map(LabReportMapper::toResponseDTO);
        }

        throw new AccessDeniedException("Access denied.");
    }

    @Override
    @Transactional(readOnly = true)
    public List<LabReportResponseDTO> getLabReportsByLabOrder(String labOrderCode) {
        LabOrder order = labOrderRepository.findByLabOrderCode(labOrderCode)
                .orElseThrow(() -> new LabOrderNotFoundException(labOrderCode));
        validateReadAccessByOrder(order);

        List<LabReport> reports = labReportRepository.findByLabOrder_LabOrderCode(labOrderCode);

        Authentication auth = getAuthentication();
        boolean isPatient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        if (isPatient) {
            // Patients see only VERIFIED results; PENDING/READY remain internal
            reports = reports.stream()
                    .filter(r -> r.getReportStatus() == ReportStatus.VERIFIED)
                    .toList();
        }
        return reports.stream().map(LabReportMapper::toResponseDTO).toList();
    }

    // ── Verify ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LabReportResponseDTO verifyLabReport(String reportCode) {
        LabReport report = labReportRepository.findByReportCode(reportCode)
                .orElseThrow(() -> new LabReportNotFoundException(reportCode));

        if (report.getReportStatus() == ReportStatus.VERIFIED) {
            throw new BusinessRuleViolationException("Report " + reportCode + " is already VERIFIED.");
        }
        if (report.getReportStatus() == ReportStatus.PENDING) {
            throw new BusinessRuleViolationException(
                "Report " + reportCode + " is still PENDING — add results before verifying.");
        }

        validateDoctorWriteAccess(report.getLabOrder());

        report.setReportStatus(ReportStatus.VERIFIED);
        LabReport saved = labReportRepository.save(report);

        log.info("Lab report verified | code={}, doctor={}",
                reportCode, report.getDoctor().getDoctorCode());

        return LabReportMapper.toResponseDTO(saved);
    }

    // ── File Upload ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReportAttachmentResponseDTO uploadAttachment(String reportCode, MultipartFile file) {
        LabReport report = labReportRepository.findByReportCode(reportCode)
                .orElseThrow(() -> new LabReportNotFoundException(reportCode));

        validateDoctorWriteAccess(report.getLabOrder());
        validateFile(file);

        String storedFileName = UUID.randomUUID() + "_" +
                sanitizeFileName(file.getOriginalFilename());

        Path uploadPath = Paths.get(uploadDirectory);
        Path filePath = uploadPath.resolve(storedFileName);

        try {
            Files.createDirectories(uploadPath);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store file for report {}: {}", reportCode, e.getMessage());
            throw new BusinessRuleViolationException(
                "File upload failed. Please try again.");
        }

        String fileUrl = uploadDirectory + "/" + storedFileName;

        ReportAttachment attachment = ReportAttachment.builder()
                .labReport(report)
                .fileName(storedFileName)
                .originalFileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .fileUrl(fileUrl)
                .build();

        ReportAttachment saved = attachmentRepository.save(attachment);

        // Promote report to READY if it was still PENDING after upload
        if (report.getReportStatus() == ReportStatus.PENDING) {
            report.setReportStatus(ReportStatus.READY);
            labReportRepository.save(report);
        }

        log.info("Attachment uploaded | report={}, file={}, size={}KB",
                reportCode, file.getOriginalFilename(), file.getSize() / 1024);

        return LabReportMapper.toAttachmentDTO(saved);
    }

    // ── List Attachments ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ReportAttachmentResponseDTO> getAttachments(String reportCode) {
        LabReport report = labReportRepository.findByReportCode(reportCode)
                .orElseThrow(() -> new LabReportNotFoundException(reportCode));
        validateReadAccess(report);
        return attachmentRepository.findByLabReport_ReportCode(reportCode).stream()
                .map(LabReportMapper::toAttachmentDTO)
                .toList();
    }

    // ── Download Attachment ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadAttachment(Long attachmentId) {
        ReportAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                    "Attachment not found: " + attachmentId));

        // Validate caller can download this report
        validateDownloadAccess(attachment.getLabReport());

        Path filePath = Paths.get(attachment.getFileUrl());
        Resource resource;
        try {
            resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessRuleViolationException(
                    "The requested file is not available. Please contact support.");
            }
        } catch (MalformedURLException e) {
            throw new BusinessRuleViolationException("Invalid file path.");
        }

        String contentType = attachment.getFileType();
        if (contentType == null) contentType = "application/octet-stream";

        log.info("Attachment downloaded | attachmentId={}, report={}, file={}",
                attachmentId, attachment.getLabReport().getReportCode(), attachment.getOriginalFileName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + sanitizeForHeader(attachment.getOriginalFileName()) + "\"")
                .body(resource);
    }

    // ── PDF Generation ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> generateLabReportPdf(String reportCode, boolean download) {
        LabReport report = labReportRepository.findByReportCode(reportCode)
                .orElseThrow(() -> new LabReportNotFoundException(reportCode));

        validatePdfAccess(report);

        HospitalSettingsResponseDTO settings = null;
        try {
            settings = hospitalSettingsService.getSettings();
        } catch (Exception ex) {
            log.warn("[LabReportPdf] Hospital settings unavailable; PDF will render without branding | report={}", reportCode);
        }

        byte[] pdfBytes = labReportPdfService.generatePdf(report, settings);

        String filename = "LAB-REPORT-" + reportCode + ".pdf";
        String disposition = download
                ? "attachment; filename=\"" + filename + "\""
                : "inline; filename=\"" + filename + "\"";

        log.info("[LabReportPdf] PDF {} | report={}, role={}",
                download ? "downloaded" : "previewed", reportCode, resolveCallerRole());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .body(pdfBytes);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private boolean hasResults(LabReportRequestDTO request) {
        return StringUtils.hasText(request.getResultValue());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidReportFileException("No file provided or file is empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidReportFileException(
                "File size " + (file.getSize() / 1024 / 1024) + " MB exceeds the 10 MB limit.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidReportFileException(
                "File type '" + contentType + "' is not allowed. " +
                "Accepted types: PDF, PNG, JPG/JPEG.");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new InvalidReportFileException("File must have a name.");
        }
        boolean validExt = ALLOWED_EXTENSIONS.stream()
                .anyMatch(originalName.toLowerCase()::endsWith);
        if (!validExt) {
            throw new InvalidReportFileException(
                "File type not allowed. Accepted formats: PDF, PNG, JPG/JPEG.");
        }
    }

    private String sanitizeFileName(String originalFilename) {
        if (originalFilename == null) return "upload";
        return originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /** Strips characters that could break an HTTP header value (CR, LF, double-quote, backslash). */
    private String sanitizeForHeader(String filename) {
        if (filename == null) return "report";
        return filename.replaceAll("[\\r\\n\"\\\\]", "_");
    }

    // ── Security Helpers ──────────────────────────────────────────────────────

    private void validateDoctorWriteAccess(LabOrder labOrder) {
        Authentication auth = getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;

        boolean isDoctor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        if (!isDoctor) throw new AccessDeniedException("Only doctors and administrators can manage lab reports.");

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String loggedInEmail = principal.getEmail();
        if (labOrder.getDoctor().getUser() == null ||
                !loggedInEmail.equals(labOrder.getDoctor().getUser().getEmail())) {
            log.warn("[Security] Lab report write violation | user={}, order={}",
                    loggedInEmail, labOrder.getLabOrderCode());
            throw new AccessDeniedException("Access denied. You can only manage reports for your own patients.");
        }
    }

    private void validateReadAccess(LabReport report) {
        Authentication auth = getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String loggedInEmail = principal.getEmail();

        boolean isDoctor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        if (isDoctor) {
            if (report.getDoctor().getUser() == null ||
                    !loggedInEmail.equals(report.getDoctor().getUser().getEmail())) {
                throw new AccessDeniedException("Access denied. You can only view your own lab reports.");
            }
            return;
        }

        boolean isPatient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        if (isPatient) {
            // Patient can only download VERIFIED reports
            if (report.getReportStatus() != ReportStatus.VERIFIED) {
                throw new AccessDeniedException(
                    "Report is not yet available. Please wait for your doctor to verify the results.");
            }
            if (report.getPatient().getUser() == null ||
                    !loggedInEmail.equals(report.getPatient().getUser().getEmail())) {
                log.warn("[Security] Patient lab report access violation | user={}, report={}",
                        loggedInEmail, report.getReportCode());
                throw new AccessDeniedException("Access denied. You can only view your own lab reports.");
            }
        }
    }

    private void validateReadAccessByOrder(LabOrder order) {
        Authentication auth = getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String loggedInEmail = principal.getEmail();

        boolean isDoctor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        if (isDoctor) {
            if (order.getDoctor().getUser() == null ||
                    !loggedInEmail.equals(order.getDoctor().getUser().getEmail())) {
                throw new AccessDeniedException("Access denied.");
            }
            return;
        }

        boolean isPatient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        if (isPatient) {
            if (order.getPatient().getUser() == null ||
                    !loggedInEmail.equals(order.getPatient().getUser().getEmail())) {
                throw new AccessDeniedException("Access denied. You can only view your own lab orders.");
            }
        }
    }

    private void validateDownloadAccess(LabReport report) {
        Authentication auth = getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String loggedInEmail = principal.getEmail();

        boolean isDoctor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        if (isDoctor) {
            if (report.getDoctor().getUser() == null ||
                    !loggedInEmail.equals(report.getDoctor().getUser().getEmail())) {
                throw new AccessDeniedException("Access denied.");
            }
            return;
        }

        boolean isPatient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        if (isPatient) {
            if (report.getReportStatus() != ReportStatus.VERIFIED) {
                throw new AccessDeniedException(
                    "This report is not yet available for download. " +
                    "Only VERIFIED reports can be downloaded by patients.");
            }
            if (report.getPatient().getUser() == null ||
                    !loggedInEmail.equals(report.getPatient().getUser().getEmail())) {
                throw new AccessDeniedException("Access denied. You can only download your own reports.");
            }
        }
    }

    /**
     * PDF access control — mirrors validateDownloadAccess but also enforces
     * VERIFIED-only for patients and logs unauthorized attempts explicitly.
     */
    private void validatePdfAccess(LabReport report) {
        Authentication auth = getAuthentication();

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String loggedInEmail = principal.getEmail();

        boolean isDoctor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DOCTOR"));
        if (isDoctor) {
            if (report.getDoctor().getUser() == null ||
                    !loggedInEmail.equals(report.getDoctor().getUser().getEmail())) {
                log.warn("[Security] PDF access denied — doctor not owner | user={}, report={}",
                        loggedInEmail, report.getReportCode());
                throw new AccessDeniedException("Access denied. You can only download PDF reports for your own patients.");
            }
            return;
        }

        boolean isPatient = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PATIENT"));
        if (isPatient) {
            if (report.getReportStatus() != ReportStatus.VERIFIED) {
                log.warn("[Security] PDF access denied — report not VERIFIED | user={}, report={}, status={}",
                        loggedInEmail, report.getReportCode(), report.getReportStatus());
                throw new AccessDeniedException(
                    "This report is not yet available for download. Only VERIFIED reports can be accessed by patients.");
            }
            if (report.getPatient().getUser() == null ||
                    !loggedInEmail.equals(report.getPatient().getUser().getEmail())) {
                log.warn("[Security] PDF access denied — patient not owner | user={}, report={}",
                        loggedInEmail, report.getReportCode());
                throw new AccessDeniedException("Access denied. You can only download your own lab reports.");
            }
            return;
        }

        throw new AccessDeniedException("Access denied.");
    }

    private String resolveCallerRole() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) return "UNKNOWN";
            return auth.getAuthorities().stream()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .findFirst().orElse("UNKNOWN");
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private Authentication getAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authentication required.");
        }
        return auth;
    }

    // ── Report Code Generation ────────────────────────────────────────────────

    /**
     * Year-scoped sequential report code.
     * Format: REP-YYYY-NNNN (e.g. REP-2026-0001)
     */
    private String generateReportCode() {
        String year = String.valueOf(LocalDate.now().getYear());
        String prefix = "REP-" + year + "-";
        Optional<LabReport> latest = labReportRepository
                .findTopByReportCodeStartingWithOrderByReportCodeDesc(prefix);
        int nextSequence = 1;
        if (latest.isPresent()) {
            String lastCode = latest.get().getReportCode();
            nextSequence = Integer.parseInt(lastCode.substring(lastCode.lastIndexOf('-') + 1)) + 1;
        }
        if (nextSequence > 9999) {
            throw new IllegalStateException(
                "Report code sequence limit (9999) reached for year " + year);
        }
        return prefix + String.format("%04d", nextSequence);
    }
}
