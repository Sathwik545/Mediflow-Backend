package com.mediflow.platform.lab.controller;

import com.mediflow.platform.common.response.ApiResponse;
import com.mediflow.platform.lab.dto.LabReportRequestDTO;
import com.mediflow.platform.lab.dto.LabReportResponseDTO;
import com.mediflow.platform.lab.dto.LabReportUpdateDTO;
import com.mediflow.platform.lab.dto.ReportAttachmentResponseDTO;
import com.mediflow.platform.lab.service.LabReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lab-reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Lab Reports", description = "Diagnostic result management and file attachments")
public class LabReportController {

    private final LabReportService labReportService;

    @PostMapping
    @Operation(summary = "Create a lab report for a specific test in an order",
               description = "ADMIN or DOCTOR. Patient and doctor are derived from the lab order.")
    public ResponseEntity<ApiResponse<LabReportResponseDTO>> createLabReport(
            @Valid @RequestBody LabReportRequestDTO request) {
        LabReportResponseDTO result = labReportService.createLabReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @PutMapping("/{reportCode}")
    @Operation(summary = "Update lab report results (ADMIN or DOCTOR)")
    public ResponseEntity<ApiResponse<LabReportResponseDTO>> updateLabReport(
            @PathVariable String reportCode,
            @RequestBody LabReportUpdateDTO request) {
        LabReportResponseDTO result = labReportService.updateLabReport(reportCode, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping
    @Operation(summary = "List all lab reports (paginated, ADMIN only)")
    public ResponseEntity<ApiResponse<Page<LabReportResponseDTO>>> getAllLabReports(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<LabReportResponseDTO> result = labReportService.getAllLabReports(search, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{reportCode}")
    @Operation(summary = "Get lab report by code")
    public ResponseEntity<ApiResponse<LabReportResponseDTO>> getLabReportByCode(
            @PathVariable String reportCode) {
        LabReportResponseDTO result = labReportService.getLabReportByCode(reportCode);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/order/{labOrderCode}")
    @Operation(summary = "Get all reports for a lab order")
    public ResponseEntity<ApiResponse<List<LabReportResponseDTO>>> getLabReportsByLabOrder(
            @PathVariable String labOrderCode) {
        List<LabReportResponseDTO> result = labReportService.getLabReportsByLabOrder(labOrderCode);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/{reportCode}/verify")
    @Operation(summary = "Verify a READY report — makes it patient-downloadable (ADMIN or DOCTOR)")
    public ResponseEntity<ApiResponse<LabReportResponseDTO>> verifyLabReport(
            @PathVariable String reportCode) {
        LabReportResponseDTO result = labReportService.verifyLabReport(reportCode);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{reportCode}/attachments")
    @Operation(summary = "Upload a file attachment to a lab report (max 10 MB; PDF, PNG, JPG)")
    public ResponseEntity<ApiResponse<ReportAttachmentResponseDTO>> uploadAttachment(
            @PathVariable String reportCode,
            @RequestParam("file") MultipartFile file) {
        ReportAttachmentResponseDTO result = labReportService.uploadAttachment(reportCode, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @GetMapping("/{reportCode}/attachments")
    @Operation(summary = "List attachments for a lab report")
    public ResponseEntity<ApiResponse<List<ReportAttachmentResponseDTO>>> getAttachments(
            @PathVariable String reportCode) {
        List<ReportAttachmentResponseDTO> result = labReportService.getAttachments(reportCode);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/attachments/{attachmentId}/download")
    @Operation(summary = "Download a report attachment — patients can only download VERIFIED reports")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        return labReportService.downloadAttachment(attachmentId);
    }

    @GetMapping("/{reportCode}/pdf")
    @Operation(
        summary = "Generate and stream a professional lab report PDF",
        description = """
            Dynamically generates a formatted lab report PDF.
            RBAC:
              ADMIN   — any report
              DOCTOR  — own patients only
              PATIENT — own VERIFIED reports only (unverified → 403)

            Query param ?download=true streams as attachment (triggers browser save dialog).
            Default (download=false) streams inline for browser preview.
            PDF is generated on each request — never stored on disk.
            """
    )
    public ResponseEntity<byte[]> generateLabReportPdf(
            @PathVariable String reportCode,
            @RequestParam(defaultValue = "false") boolean download) {
        return labReportService.generateLabReportPdf(reportCode, download);
    }
}
