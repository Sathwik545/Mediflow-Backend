package com.mediflow.platform.lab.service;

import com.mediflow.platform.lab.dto.LabReportRequestDTO;
import com.mediflow.platform.lab.dto.LabReportResponseDTO;
import com.mediflow.platform.lab.dto.LabReportUpdateDTO;
import com.mediflow.platform.lab.dto.ReportAttachmentResponseDTO;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface LabReportService {

    LabReportResponseDTO createLabReport(LabReportRequestDTO request);

    LabReportResponseDTO updateLabReport(String reportCode, LabReportUpdateDTO request);

    LabReportResponseDTO getLabReportByCode(String reportCode);

    Page<LabReportResponseDTO> getAllLabReports(String search, int page, int size);

    List<LabReportResponseDTO> getLabReportsByLabOrder(String labOrderCode);

    LabReportResponseDTO verifyLabReport(String reportCode);

    ReportAttachmentResponseDTO uploadAttachment(String reportCode, MultipartFile file);

    List<ReportAttachmentResponseDTO> getAttachments(String reportCode);

    ResponseEntity<Resource> downloadAttachment(Long attachmentId);

    /**
     * Generates a professional lab report PDF in-memory and streams it to the client.
     *
     * RBAC enforced in implementation:
     *   ADMIN   — any report
     *   DOCTOR  — own patients' reports only
     *   PATIENT — own VERIFIED reports only (unverified → 403)
     *
     * @param reportCode the lab report code (REP-YYYY-NNNN)
     * @param download   true → Content-Disposition: attachment; false → inline (preview)
     * @return streaming PDF response
     */
    ResponseEntity<byte[]> generateLabReportPdf(String reportCode, boolean download);
}
