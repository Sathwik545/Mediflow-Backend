package com.mediflow.platform.lab.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import com.mediflow.platform.lab.entity.LabReport;
import com.mediflow.platform.lab.enums.ReportStatus;
import com.mediflow.platform.settings.dto.HospitalSettingsResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

/**
 * Generates professional, printer-friendly lab report PDFs in-memory using OpenPDF 1.3.x.
 *
 * OpenPDF 1.3.x API notes (same as InvoicePdfService):
 *  — Colors: java.awt.Color (NOT BaseColor — that is iText 5.x)
 *  — Fonts:  Font(int family, float size, int style, Color color)
 *            Font.HELVETICA, Font.BOLD, Font.ITALIC, Font.NORMAL
 *
 * PDFs are generated dynamically — never stored on disk or in the database.
 * All organization details come from HospitalSettingsResponseDTO (never hardcoded).
 *
 * PDF Structure:
 *   1. Hospital header band (teal/dark)
 *   2. Report metadata (report code, order code, generated at, status)
 *   3. Patient information (name, code, gender, DOB, age, blood group)
 *   4. Doctor information (name, code, specialization, consultation)
 *   5. Test results table (test name, category, result, range, abnormal flag, remarks)
 *   6. Interpretation section (if present)
 *   7. Verification section (verified by, verified at, signature placeholder)
 *   8. Footer (computer-generated disclaimer, page numbers, generated at)
 */
@Component
@Slf4j
public class LabReportPdfService {

    // ── Colour palette (java.awt.Color — OpenPDF 1.3.x) ─────────────────────
    private static final Color COL_TEAL        = new Color(15, 118, 110);   // #0f766e teal-700
    private static final Color COL_TEAL_DARK   = new Color(4, 47, 46);     // #042f2e teal-950
    private static final Color COL_TEAL_BG     = new Color(240, 253, 250); // #f0fdf4 teal-50
    private static final Color COL_TEAL_LIGHT  = new Color(153, 246, 228); // #99f6e4 teal-200
    private static final Color COL_SECTION_BG  = new Color(248, 250, 252); // slate-50
    private static final Color COL_BORDER      = new Color(226, 232, 240); // slate-200
    private static final Color COL_BODY        = new Color(15, 23, 42);    // slate-900
    private static final Color COL_LABEL       = new Color(100, 116, 139); // slate-500
    private static final Color COL_MUTED       = new Color(148, 163, 184); // slate-400
    private static final Color COL_WHITE       = Color.WHITE;
    private static final Color COL_RED         = new Color(220, 38, 38);   // red-600
    private static final Color COL_GREEN       = new Color(22, 163, 74);   // green-600
    private static final Color COL_AMBER       = new Color(180, 83, 9);    // amber-700
    private static final Color COL_HEADER_SUB  = new Color(204, 251, 241); // teal-100

    // ── Formatters ────────────────────────────────────────────────────────────
    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates a professional lab report PDF in-memory.
     *
     * @param report   fully loaded LabReport with patient, doctor, labOrder, labOrderItem
     * @param settings hospital organization settings (may be null — renders without branding)
     * @return raw PDF bytes ready to be streamed as application/pdf
     */
    public byte[] generatePdf(LabReport report, HospitalSettingsResponseDTO settings) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 40f, 40f, 40f, 40f);
            PdfWriter.getInstance(document, baos);
            document.open();

            addHospitalHeader(document, report, settings);
            spacer(document, 10);
            addReportMetaSection(document, report);
            spacer(document, 10);
            addPatientDoctorSection(document, report);
            spacer(document, 10);
            addTestResultsSection(document, report);

            if (hasText(report.getInterpretation())) {
                spacer(document, 10);
                addInterpretationSection(document, report);
            }

            spacer(document, 10);
            addVerificationSection(document, report);
            spacer(document, 18);
            addFooter(document, report, settings);

            document.close();
            log.info("[LabReportPdf] PDF generated | report={}", report.getReportCode());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("[LabReportPdf] PDF generation failed | report={}", report.getReportCode(), e);
            throw new RuntimeException("Lab report PDF generation failed: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Section builders
    // ─────────────────────────────────────────────────────────────────────────

    /** Full-width teal header band: hospital info left, "LAB REPORT" label right. */
    private void addHospitalHeader(Document doc, LabReport report, HospitalSettingsResponseDTO s)
            throws DocumentException {
        PdfPTable header = new PdfPTable(new float[]{3f, 1.4f});
        header.setWidthPercentage(100f);

        Font nameFont   = font(14, Font.BOLD,   COL_WHITE);
        Font infoFont   = font(8.5f, Font.NORMAL, COL_HEADER_SUB);
        Font smallFont  = font(7.5f, Font.NORMAL, COL_TEAL_LIGHT);
        Font titleFont  = font(18, Font.BOLD,   COL_WHITE);
        Font subFont    = font(8,  Font.NORMAL, COL_TEAL_LIGHT);

        // ── Left cell: hospital details ───────────────────────────────────────
        PdfPCell left = new PdfPCell();
        left.setBackgroundColor(COL_TEAL_DARK);
        left.setBorder(Rectangle.NO_BORDER);
        left.setPadding(18f);

        String hospName = s != null && hasText(s.getHospitalName()) ? s.getHospitalName() : "MediFlow Hospital";
        Paragraph hospNamePara = new Paragraph(hospName, nameFont);
        hospNamePara.setSpacingAfter(6f);
        left.addElement(hospNamePara);

        if (s != null) {
            String address = buildAddressLine(s);
            if (!address.isEmpty()) {
                Paragraph addr = new Paragraph(address, infoFont);
                addr.setSpacingAfter(3f);
                left.addElement(addr);
            }
            String contact = buildContactLine(s);
            if (!contact.isEmpty()) {
                Paragraph cont = new Paragraph(contact, infoFont);
                cont.setSpacingAfter(3f);
                left.addElement(cont);
            }
            if (hasText(s.getWebsite())) {
                left.addElement(new Paragraph(s.getWebsite(), infoFont));
            }
            if (hasText(s.getGstNumber())) {
                Paragraph gst = new Paragraph("GST: " + s.getGstNumber(), smallFont);
                gst.setSpacingBefore(3f);
                left.addElement(gst);
            }
        }

        // ── Right cell: LAB REPORT label ──────────────────────────────────────
        PdfPCell right = new PdfPCell();
        right.setBackgroundColor(COL_TEAL_DARK);
        right.setBorder(Rectangle.NO_BORDER);
        right.setPadding(18f);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph title = new Paragraph("LAB REPORT", titleFont);
        title.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(title);

        Paragraph sub = new Paragraph("DIAGNOSTIC RESULT", subFont);
        sub.setAlignment(Element.ALIGN_RIGHT);
        sub.setSpacingBefore(4f);
        right.addElement(sub);

        // Report code below title
        Paragraph codeP = new Paragraph(report.getReportCode(), font(10, Font.BOLD, COL_TEAL_LIGHT));
        codeP.setAlignment(Element.ALIGN_RIGHT);
        codeP.setSpacingBefore(8f);
        right.addElement(codeP);

        header.addCell(left);
        header.addCell(right);
        doc.add(header);
    }

    /** 4-column metadata grid: report code, order code, generated at, status. */
    private void addReportMetaSection(Document doc, LabReport report) throws DocumentException {
        Font labelFont = font(7.5f, Font.BOLD,   COL_LABEL);
        Font codeFont  = font(10f,  Font.BOLD,   COL_TEAL);
        Font valFont   = font(9f,   Font.NORMAL, COL_BODY);

        PdfPTable table = new PdfPTable(new float[]{1f, 1.8f, 1f, 1.8f});
        table.setWidthPercentage(100f);

        // Row 1 — Report Code | Order Code
        metaLabelCell(table, "REPORT CODE", labelFont);
        metaValueCell(table, safe(report.getReportCode()), codeFont);
        metaLabelCell(table, "ORDER CODE", labelFont);
        metaValueCell(table, safe(report.getLabOrder().getLabOrderCode()), valFont);

        // Row 2 — Report Date | Report Status
        metaLabelCell(table, "REPORT DATE", labelFont);
        metaValueCell(table,
                report.getCreatedAt() != null ? report.getCreatedAt().format(DATE_FMT) : "—",
                valFont);
        metaLabelCell(table, "STATUS", labelFont);
        String statusText = report.getReportStatus() != null ? report.getReportStatus().name() : "—";
        Color statusColor = report.getReportStatus() == ReportStatus.VERIFIED ? COL_GREEN
                : report.getReportStatus() == ReportStatus.READY ? COL_AMBER : COL_LABEL;
        metaValueCell(table, statusText, font(9f, Font.BOLD, statusColor));

        // Row 3 — Last Updated | Test Name
        metaLabelCell(table, "LAST UPDATED", labelFont);
        metaValueCell(table,
                report.getUpdatedAt() != null ? report.getUpdatedAt().format(DATETIME_FMT) : "—",
                valFont);
        metaLabelCell(table, "TEST NAME", labelFont);
        metaValueCell(table, safe(report.getLabOrderItem().getTestName()), valFont);

        doc.add(table);
    }

    /** Two-column section: patient details left, doctor details right. */
    private void addPatientDoctorSection(Document doc, LabReport report) throws DocumentException {
        sectionHeader(doc, "PATIENT & DOCTOR INFORMATION");

        Font labelFont = font(7.5f, Font.BOLD,   COL_LABEL);
        Font nameFont  = font(11f,  Font.BOLD,   COL_BODY);
        Font valFont   = font(9.5f, Font.NORMAL, COL_BODY);

        PdfPTable table = new PdfPTable(new float[]{1f, 1f});
        table.setWidthPercentage(100f);

        // ── Patient cell ──────────────────────────────────────────────────────
        PdfPCell patCell = new PdfPCell();
        patCell.setBorderColor(COL_BORDER);
        patCell.setBorderWidth(0.5f);
        patCell.setPadding(12f);

        patCell.addElement(para("PATIENT", labelFont));
        String patName = resolvePatientName(report);
        patCell.addElement(para(patName, nameFont));
        patCell.addElement(spacerPara(6f));

        patCell.addElement(para("Patient Code", labelFont));
        patCell.addElement(para(safe(report.getPatient().getPatientCode()), valFont));
        patCell.addElement(spacerPara(4f));

        if (report.getPatient().getGender() != null) {
            patCell.addElement(para("Gender", labelFont));
            patCell.addElement(para(fmtGender(report.getPatient().getGender().name()), valFont));
            patCell.addElement(spacerPara(4f));
        }

        if (report.getPatient().getDateOfBirth() != null) {
            LocalDate dob = report.getPatient().getDateOfBirth();
            patCell.addElement(para("Date of Birth", labelFont));
            patCell.addElement(para(dob.format(DATE_FMT), valFont));
            patCell.addElement(spacerPara(4f));

            int age = Period.between(dob, LocalDate.now()).getYears();
            patCell.addElement(para("Age", labelFont));
            patCell.addElement(para(age + " years", valFont));
            patCell.addElement(spacerPara(4f));
        }

        if (report.getPatient().getBloodGroup() != null) {
            patCell.addElement(para("Blood Group", labelFont));
            patCell.addElement(para(fmtBloodGroup(report.getPatient().getBloodGroup().name()), valFont));
        }

        // ── Doctor cell ───────────────────────────────────────────────────────
        PdfPCell docCell = new PdfPCell();
        docCell.setBorderColor(COL_BORDER);
        docCell.setBorderWidthTop(0.5f);
        docCell.setBorderWidthBottom(0.5f);
        docCell.setBorderWidthRight(0.5f);
        docCell.setBorderWidthLeft(0f);
        docCell.setPadding(12f);
        docCell.setPaddingLeft(14f);

        docCell.addElement(para("ORDERING DOCTOR", labelFont));
        String docName = resolveDoctorName(report);
        docCell.addElement(para("Dr. " + docName, nameFont));
        docCell.addElement(spacerPara(6f));

        docCell.addElement(para("Doctor Code", labelFont));
        docCell.addElement(para(safe(report.getDoctor().getDoctorCode()), valFont));
        docCell.addElement(spacerPara(4f));

        if (hasText(report.getDoctor().getSpecialization())) {
            docCell.addElement(para("Specialization", labelFont));
            docCell.addElement(para(report.getDoctor().getSpecialization(), valFont));
            docCell.addElement(spacerPara(4f));
        }

        if (report.getLabOrder() != null && report.getLabOrder().getConsultation() != null) {
            String consultCode = report.getLabOrder().getConsultation().getConsultationCode();
            if (hasText(consultCode)) {
                docCell.addElement(para("Consultation Ref.", labelFont));
                docCell.addElement(para(consultCode, valFont));
            }
        }

        table.addCell(patCell);
        table.addCell(docCell);
        doc.add(table);
    }

    /** Test results table: test name, category, result, unit/range, abnormal flag, remarks. */
    private void addTestResultsSection(Document doc, LabReport report) throws DocumentException {
        sectionHeader(doc, "TEST RESULTS");

        Font colHdrFont  = font(8f,   Font.BOLD,   COL_LABEL);
        Font cellFont    = font(9f,   Font.NORMAL, COL_BODY);
        Font abnFont     = font(9f,   Font.BOLD,   COL_RED);
        Font noDataFont  = font(9f,   Font.ITALIC, COL_LABEL);

        // Columns: Test Name | Category | Result | Reference Range | Remarks
        PdfPTable table = new PdfPTable(new float[]{2.2f, 1.4f, 1.6f, 1.8f, 2f});
        table.setWidthPercentage(100f);

        // Column headers
        tableHeader(table, "TEST NAME",       colHdrFont);
        tableHeader(table, "CATEGORY",        colHdrFont);
        tableHeader(table, "RESULT",          colHdrFont);
        tableHeader(table, "REFERENCE RANGE", colHdrFont);
        tableHeader(table, "REMARKS",         colHdrFont);

        if (!hasText(report.getResultValue())) {
            // No results yet — single merged row
            PdfPCell noData = new PdfPCell(new Phrase("No test results recorded.", noDataFont));
            noData.setColspan(5);
            noData.setBorderColor(COL_BORDER);
            noData.setBorderWidthBottom(0.5f);
            noData.setBorderWidthTop(0f);
            noData.setBorderWidthLeft(0.5f);
            noData.setBorderWidthRight(0.5f);
            noData.setPadding(12f);
            noData.setPaddingLeft(14f);
            noData.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(noData);
        } else {
            boolean isAbnormal = Boolean.TRUE.equals(report.getAbnormalFlag());
            Font resultFont = isAbnormal ? abnFont : cellFont;

            String resultDisplay = report.getResultValue();
            if (isAbnormal) resultDisplay += "  ⚠ ABNORMAL";

            tableDataCell(table, safe(report.getLabOrderItem().getTestName()), cellFont, false);
            tableDataCell(table, safe(report.getLabOrderItem().getCategory()), cellFont, false);
            tableDataCell(table, resultDisplay, resultFont, isAbnormal);
            tableDataCell(table, safe(report.getReferenceRange()), cellFont, false);
            tableDataCell(table, safe(report.getRemarks()), cellFont, false);
        }

        doc.add(table);
    }

    /** Clinical interpretation paragraph, shown only when interpretation is present. */
    private void addInterpretationSection(Document doc, LabReport report) throws DocumentException {
        sectionHeader(doc, "CLINICAL INTERPRETATION");

        Font valFont = font(9.5f, Font.NORMAL, COL_BODY);

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100f);

        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(COL_BORDER);
        cell.setBorderWidthTop(0f);
        cell.setBorderWidthBottom(0.5f);
        cell.setBorderWidthLeft(0.5f);
        cell.setBorderWidthRight(0.5f);
        cell.setPadding(12f);
        cell.setPaddingLeft(14f);

        Paragraph p = new Paragraph(report.getInterpretation(), valFont);
        p.setLeading(14f);
        cell.addElement(p);
        table.addCell(cell);
        doc.add(table);
    }

    /** Verification status, verified by/at, and digital signature placeholder. */
    private void addVerificationSection(Document doc, LabReport report) throws DocumentException {
        sectionHeader(doc, "VERIFICATION");

        Font labelFont   = font(7.5f, Font.BOLD,   COL_LABEL);
        Font valFont     = font(9.5f, Font.NORMAL, COL_BODY);
        Font verFont     = font(11f,  Font.BOLD,   COL_GREEN);
        Font pendFont    = font(11f,  Font.BOLD,   COL_AMBER);
        Font sigFont     = font(8.5f, Font.ITALIC, COL_MUTED);

        PdfPTable table = new PdfPTable(new float[]{1f, 1f, 1f});
        table.setWidthPercentage(100f);

        // Status cell
        PdfPCell statusCell = borderedCell(COL_TEAL_BG);
        statusCell.addElement(para("VERIFICATION STATUS", labelFont));
        boolean isVerified = report.getReportStatus() == ReportStatus.VERIFIED;
        Font stFont = isVerified ? verFont : pendFont;
        String stLabel = isVerified ? "✔  VERIFIED" :
                (report.getReportStatus() == ReportStatus.READY ? "READY FOR REVIEW" : "PENDING");
        statusCell.addElement(para(stLabel, stFont));

        // Verified By cell
        PdfPCell byCell = borderedCell(COL_WHITE);
        byCell.setBorderWidthLeft(0f);
        byCell.addElement(para("VERIFIED BY", labelFont));
        if (isVerified && hasText(report.getUpdatedBy())) {
            byCell.addElement(para(report.getUpdatedBy(), valFont));
        } else {
            byCell.addElement(para("—", font(9.5f, Font.ITALIC, COL_MUTED)));
        }

        // Verified At cell
        PdfPCell atCell = borderedCell(COL_WHITE);
        atCell.setBorderWidthLeft(0f);
        atCell.addElement(para("VERIFIED AT", labelFont));
        if (isVerified && report.getUpdatedAt() != null) {
            atCell.addElement(para(report.getUpdatedAt().format(DATETIME_FMT), valFont));
        } else {
            atCell.addElement(para("—", font(9.5f, Font.ITALIC, COL_MUTED)));
        }

        table.addCell(statusCell);
        table.addCell(byCell);
        table.addCell(atCell);
        doc.add(table);

        spacer(doc, 8);

        // Digital signature placeholder
        PdfPTable sigTable = new PdfPTable(new float[]{1f, 2f});
        sigTable.setWidthPercentage(100f);

        PdfPCell sigLbl = new PdfPCell();
        sigLbl.setBorder(Rectangle.NO_BORDER);
        sigLbl.setPadding(4f);
        sigLbl.addElement(para("AUTHORIZING DOCTOR", labelFont));
        sigLbl.addElement(para(isVerified ? "Dr. " + resolveDoctorName(report) : "—",
                font(10f, Font.BOLD, COL_BODY)));

        PdfPCell sigBox = new PdfPCell();
        sigBox.setBorderColor(COL_BORDER);
        sigBox.setBorderWidth(0.5f);
        sigBox.setPadding(16f);
        sigBox.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph sigP = new Paragraph("[ Digital Signature ]", sigFont);
        sigP.setAlignment(Element.ALIGN_CENTER);
        sigBox.addElement(sigP);

        sigTable.addCell(sigLbl);
        sigTable.addCell(sigBox);
        doc.add(sigTable);
    }

    /** Footer: separator + disclaimer + audit + hospital code. */
    private void addFooter(Document doc, LabReport report, HospitalSettingsResponseDTO s)
            throws DocumentException {
        LineSeparator sep = new LineSeparator(0.5f, 100f, COL_BORDER, Element.ALIGN_CENTER, 0f);
        doc.add(new Chunk(sep));
        spacer(doc, 8);

        Font footerFont = font(8f,  Font.ITALIC, COL_LABEL);
        Font smallFont  = font(7f,  Font.NORMAL, COL_MUTED);

        Paragraph line1 = new Paragraph(
                "This is a computer-generated medical report. No physical signature is required.", footerFont);
        line1.setAlignment(Element.ALIGN_CENTER);
        doc.add(line1);

        Paragraph line2 = new Paragraph(
                "Confidential — For authorized personnel and the named patient only.", smallFont);
        line2.setAlignment(Element.ALIGN_CENTER);
        line2.setSpacingBefore(2f);
        doc.add(line2);

        if (report.getCreatedAt() != null) {
            String generated = "Report generated: " + report.getCreatedAt().format(DATETIME_FMT)
                    + "   |   Report code: " + report.getReportCode();
            Paragraph line3 = new Paragraph(generated, smallFont);
            line3.setAlignment(Element.ALIGN_CENTER);
            line3.setSpacingBefore(3f);
            doc.add(line3);
        }

        if (s != null && hasText(s.getHospitalName())) {
            String hospLine = s.getHospitalName();
            if (hasText(s.getHospitalCode())) hospLine += " — " + s.getHospitalCode();
            if (hasText(s.getSupportEmail()))  hospLine += "   |   " + s.getSupportEmail();
            Paragraph line4 = new Paragraph(hospLine, smallFont);
            line4.setAlignment(Element.ALIGN_CENTER);
            line4.setSpacingBefore(2f);
            doc.add(line4);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cell / table helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void sectionHeader(Document doc, String title) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100f);
        Font f = font(8.5f, Font.BOLD, COL_TEAL_DARK);
        PdfPCell cell = new PdfPCell(new Phrase(title, f));
        cell.setBackgroundColor(COL_SECTION_BG);
        cell.setBorderColor(COL_BORDER);
        cell.setBorderWidthBottom(0f);
        cell.setBorderWidthTop(0.5f);
        cell.setBorderWidthLeft(0.5f);
        cell.setBorderWidthRight(0.5f);
        cell.setPadding(8f);
        cell.setPaddingLeft(12f);
        t.addCell(cell);
        doc.add(t);
    }

    private void metaLabelCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COL_SECTION_BG);
        cell.setBorderColor(COL_BORDER);
        cell.setBorderWidthBottom(0.5f);
        cell.setBorderWidthTop(0f);
        cell.setBorderWidthLeft(0f);
        cell.setBorderWidthRight(0f);
        cell.setPadding(8f);
        cell.setPaddingLeft(10f);
        cell.addElement(new Paragraph(text, font));
        table.addCell(cell);
    }

    private void metaValueCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(COL_BORDER);
        cell.setBorderWidthBottom(0.5f);
        cell.setBorderWidthTop(0f);
        cell.setBorderWidthLeft(0f);
        cell.setBorderWidthRight(0f);
        cell.setPadding(8f);
        cell.setPaddingLeft(6f);
        cell.addElement(new Paragraph(text, font));
        table.addCell(cell);
    }

    private void tableHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(COL_SECTION_BG);
        cell.setBorderColor(COL_BORDER);
        cell.setBorderWidthBottom(0.5f);
        cell.setBorderWidthTop(0f);
        cell.setBorderWidthLeft(0.5f);
        cell.setBorderWidthRight(0f);
        cell.setPadding(8f);
        cell.setPaddingLeft(8f);
        table.addCell(cell);
    }

    private void tableDataCell(PdfPTable table, String text, Font font, boolean highlight) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(COL_BORDER);
        cell.setBorderWidthBottom(0.5f);
        cell.setBorderWidthTop(0f);
        cell.setBorderWidthLeft(0.5f);
        cell.setBorderWidthRight(0f);
        cell.setPadding(9f);
        cell.setPaddingLeft(8f);
        if (highlight) cell.setBackgroundColor(new Color(254, 242, 242)); // red-50
        Paragraph p = new Paragraph(text, font);
        p.setLeading(13f);
        cell.addElement(p);
        table.addCell(cell);
    }

    private PdfPCell borderedCell(Color bg) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setBorderColor(COL_BORDER);
        cell.setBorderWidthTop(0f);
        cell.setBorderWidthBottom(0.5f);
        cell.setBorderWidthLeft(0.5f);
        cell.setBorderWidthRight(0.5f);
        cell.setPadding(12f);
        return cell;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String resolvePatientName(LabReport report) {
        // Prefer snapshot (historical accuracy); fall back to live entity
        if (hasText(report.getPatientNameSnapshot())) return report.getPatientNameSnapshot();
        return report.getPatient().getFirstName() + " " + report.getPatient().getLastName();
    }

    private String resolveDoctorName(LabReport report) {
        if (hasText(report.getDoctorNameSnapshot())) return report.getDoctorNameSnapshot();
        return report.getDoctor().getFirstName() + " " + report.getDoctor().getLastName();
    }

    private String buildAddressLine(HospitalSettingsResponseDTO s) {
        StringBuilder sb = new StringBuilder();
        if (hasText(s.getAddressLine1())) sb.append(s.getAddressLine1());
        if (hasText(s.getAddressLine2())) sb.append(", ").append(s.getAddressLine2());
        if (hasText(s.getCity()))         sb.append(", ").append(s.getCity());
        if (hasText(s.getState()))        sb.append(", ").append(s.getState());
        if (hasText(s.getPostalCode()))   sb.append(" ").append(s.getPostalCode());
        if (hasText(s.getCountry()))      sb.append(", ").append(s.getCountry());
        return sb.toString();
    }

    private String buildContactLine(HospitalSettingsResponseDTO s) {
        StringBuilder sb = new StringBuilder();
        if (hasText(s.getPhoneNumber())) sb.append("Tel: ").append(s.getPhoneNumber());
        if (hasText(s.getEmail())) {
            if (sb.length() > 0) sb.append("   |   ");
            sb.append(s.getEmail());
        }
        return sb.toString();
    }

    private String fmtGender(String g) {
        if (g == null) return "—";
        return switch (g.toUpperCase()) {
            case "MALE"   -> "Male";
            case "FEMALE" -> "Female";
            case "OTHER"  -> "Other";
            default       -> g;
        };
    }

    private String fmtBloodGroup(String bg) {
        if (bg == null) return "—";
        return bg.replace("_POSITIVE", " +ve").replace("_NEGATIVE", " -ve");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Formatting helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Font font(float size, int style, Color color) {
        return new Font(Font.HELVETICA, size, style, color);
    }

    private Paragraph para(String text, Font f) {
        return new Paragraph(safe(text), f);
    }

    private Paragraph spacerPara(float leading) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(leading);
        return p;
    }

    private void spacer(Document doc, int height) throws DocumentException {
        Paragraph p = new Paragraph(" ");
        p.setLeading(height);
        doc.add(p);
    }

    private String safe(String value) {
        return (value != null && !value.isBlank()) ? value : "—";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
