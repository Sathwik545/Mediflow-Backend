package com.mediflow.platform.billing.invoice.pdf;

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
import com.mediflow.platform.billing.invoice.dto.InvoiceReceiptDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Generates professional, printer-friendly invoice PDFs in-memory using OpenPDF 1.3.x.
 *
 * OpenPDF 1.3.x API notes (differs from iText 5.x):
 *  — Colors:  java.awt.Color  (NOT BaseColor — that is an iText 5.x concept)
 *  — Fonts:   Font(int family, float size, int style, Color color)
 *             family constants: Font.HELVETICA, Font.COURIER, Font.TIMES_ROMAN
 *             style  constants: Font.NORMAL, Font.BOLD, Font.ITALIC, Font.BOLDITALIC
 *
 * PDFs are generated dynamically — never stored on disk or in the database.
 * All organization details are consumed from the InvoiceReceiptDTO, which is
 * populated exclusively from HospitalSettings (never hardcoded).
 *
 * PDF Structure:
 *   1. Hospital header band (navy)
 *   2. Invoice metadata (Invoice #, Bill Code, Appointment, Dates, Payment Method)
 *   3. Patient & Doctor details (two-column)
 *   4. Consultation details (type, date, time)
 *   5. Payment summary (fee, tax, discount, total)
 *   6. Footer (computer-generated disclaimer + audit info)
 */
@Component
@Slf4j
public class InvoicePdfService {

    // ── Colour palette (java.awt.Color — OpenPDF 1.3.x) ─────────────────────
    private static final Color COL_NAVY       = new Color(30, 58, 95);
    private static final Color COL_INDIGO     = new Color(79, 70, 229);
    private static final Color COL_INDIGO_BG  = new Color(238, 242, 255);
    private static final Color COL_SECTION_BG = new Color(248, 250, 252);
    private static final Color COL_BORDER     = new Color(226, 232, 240);
    private static final Color COL_BODY       = new Color(15, 23, 42);
    private static final Color COL_LABEL      = new Color(100, 116, 139);
    private static final Color COL_MUTED      = new Color(148, 163, 184);
    private static final Color COL_GREEN      = new Color(22, 163, 74);
    private static final Color COL_WHITE      = Color.WHITE;
    private static final Color COL_HEADER_SUB = new Color(203, 213, 225);

    // ── Date formatters ───────────────────────────────────────────────────────
    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates a professional invoice PDF in-memory.
     *
     * @param dto  fully populated InvoiceReceiptDTO (assembled by InvoiceServiceImpl)
     * @return     raw PDF bytes ready to be streamed as application/pdf
     */
    public byte[] generateInvoicePdf(InvoiceReceiptDTO dto) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // A4 page: margins left=40, right=40, top=40, bottom=40 (points)
            Document document = new Document(PageSize.A4, 40f, 40f, 40f, 40f);
            PdfWriter.getInstance(document, baos);
            document.open();

            addHospitalHeader(document, dto);
            spacer(document, 10);
            addInvoiceMetaSection(document, dto);
            spacer(document, 10);
            addPatientDoctorSection(document, dto);
            spacer(document, 10);
            addConsultationSection(document, dto);
            spacer(document, 10);
            addPaymentSection(document, dto);
            spacer(document, 18);
            addFooter(document, dto);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("[InvoicePdf] PDF generation failed for invoice={}", dto.getInvoiceNumber(), e);
            throw new RuntimeException("Invoice PDF generation failed: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Section builders
    // ─────────────────────────────────────────────────────────────────────────

    /** Full-width navy header band: hospital info on left, "INVOICE" label on right. */
    private void addHospitalHeader(Document doc, InvoiceReceiptDTO dto) throws DocumentException {
        PdfPTable header = new PdfPTable(new float[]{3f, 1.2f});
        header.setWidthPercentage(100f);

        Font nameFont    = font(14, Font.BOLD,   COL_WHITE);
        Font infoFont    = font(8.5f, Font.NORMAL, COL_HEADER_SUB);
        Font smallFont   = font(7.5f, Font.NORMAL, COL_MUTED);
        Font invFont     = font(20, Font.BOLD,   COL_WHITE);
        Font receiptFont = font(8,  Font.NORMAL, COL_MUTED);

        // ── Left cell: hospital details ───────────────────────────────────────
        PdfPCell left = new PdfPCell();
        left.setBackgroundColor(COL_NAVY);
        left.setBorder(Rectangle.NO_BORDER);
        left.setPadding(18f);

        Paragraph hospName = new Paragraph(safe(dto.getHospitalName()), nameFont);
        hospName.setSpacingAfter(6f);
        left.addElement(hospName);

        String address = buildAddressLine(dto);
        if (!address.isEmpty()) {
            Paragraph addr = new Paragraph(address, infoFont);
            addr.setSpacingAfter(3f);
            left.addElement(addr);
        }

        String contact = buildContactLine(dto);
        if (!contact.isEmpty()) {
            Paragraph cont = new Paragraph(contact, infoFont);
            cont.setSpacingAfter(3f);
            left.addElement(cont);
        }

        if (dto.getGstNumber() != null) {
            left.addElement(new Paragraph("GST: " + dto.getGstNumber(), smallFont));
        }

        // ── Right cell: INVOICE label ─────────────────────────────────────────
        PdfPCell right = new PdfPCell();
        right.setBackgroundColor(COL_NAVY);
        right.setBorder(Rectangle.NO_BORDER);
        right.setPadding(18f);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph inv = new Paragraph("INVOICE", invFont);
        inv.setAlignment(Element.ALIGN_RIGHT);
        right.addElement(inv);

        Paragraph rec = new Paragraph("RECEIPT / BILL", receiptFont);
        rec.setAlignment(Element.ALIGN_RIGHT);
        rec.setSpacingBefore(4f);
        right.addElement(rec);

        header.addCell(left);
        header.addCell(right);
        doc.add(header);
    }

    /**
     * Invoice metadata: 4-column grid (label | value | label | value).
     * Shows Invoice Number, Bill Code, Appointment Code, Generated Date, Paid On, Payment Method.
     */
    private void addInvoiceMetaSection(Document doc, InvoiceReceiptDTO dto) throws DocumentException {
        Font labelFont = font(7.5f, Font.BOLD,   COL_LABEL);
        Font codeFont  = font(10f,  Font.BOLD,   COL_INDIGO);
        Font valFont   = font(9f,   Font.NORMAL, COL_BODY);

        PdfPTable table = new PdfPTable(new float[]{1f, 1.8f, 1f, 1.8f});
        table.setWidthPercentage(100f);

        // Row 1 — Invoice Number | Bill Code
        metaLabelCell(table, "INVOICE NUMBER", labelFont);
        metaValueCell(table, safe(dto.getInvoiceNumber()), codeFont);
        metaLabelCell(table, "BILL CODE", labelFont);
        metaValueCell(table, safe(dto.getBillCode()), valFont);

        // Row 2 — Appointment | Date Generated
        metaLabelCell(table, "APPOINTMENT", labelFont);
        metaValueCell(table, safe(dto.getAppointmentCode()), valFont);
        metaLabelCell(table, "DATE GENERATED", labelFont);
        metaValueCell(table,
                dto.getGeneratedDate() != null ? dto.getGeneratedDate().format(DATE_FMT) : "—",
                valFont);

        // Row 3 — Paid On | Payment Method
        metaLabelCell(table, "PAID ON", labelFont);
        metaValueCell(table,
                dto.getPaidDate() != null ? dto.getPaidDate().format(DATETIME_FMT) : "—",
                valFont);
        metaLabelCell(table, "PAYMENT METHOD", labelFont);
        metaValueCell(table, fmtPaymentMethod(dto.getPaymentMethod()), valFont);

        doc.add(table);
    }

    /** Two-column section: patient details on left, doctor details on right. */
    private void addPatientDoctorSection(Document doc, InvoiceReceiptDTO dto) throws DocumentException {
        sectionHeader(doc, "PATIENT & DOCTOR DETAILS");

        Font labelFont = font(7.5f, Font.BOLD,   COL_LABEL);
        Font nameFont  = font(11f,  Font.BOLD,   COL_BODY);
        Font valFont   = font(9.5f, Font.NORMAL, COL_BODY);

        PdfPTable table = new PdfPTable(new float[]{1f, 1f});
        table.setWidthPercentage(100f);

        // Patient cell
        PdfPCell patCell = new PdfPCell();
        patCell.setBorderColor(COL_BORDER);
        patCell.setBorderWidthRight(0.5f);
        patCell.setBorderWidthTop(0.5f);
        patCell.setBorderWidthBottom(0.5f);
        patCell.setBorderWidthLeft(0.5f);
        patCell.setPadding(12f);

        patCell.addElement(para("PATIENT", labelFont));
        patCell.addElement(para(safe(dto.getPatientName()), nameFont));
        patCell.addElement(spacerPara(6f));
        patCell.addElement(para("Code", labelFont));
        patCell.addElement(para(safe(dto.getPatientCode()), valFont));
        if (dto.getPatientPhone() != null) {
            patCell.addElement(spacerPara(6f));
            patCell.addElement(para("Contact", labelFont));
            patCell.addElement(para(dto.getPatientPhone(), valFont));
        }

        // Doctor cell
        PdfPCell docCell = new PdfPCell();
        docCell.setBorderColor(COL_BORDER);
        docCell.setBorderWidthRight(0.5f);
        docCell.setBorderWidthTop(0.5f);
        docCell.setBorderWidthBottom(0.5f);
        docCell.setBorderWidthLeft(0f);
        docCell.setPadding(12f);
        docCell.setPaddingLeft(14f);

        docCell.addElement(para("ATTENDING DOCTOR", labelFont));
        docCell.addElement(para(safe(dto.getDoctorName()), nameFont));
        if (dto.getDepartment() != null) {
            docCell.addElement(spacerPara(6f));
            docCell.addElement(para("Department", labelFont));
            docCell.addElement(para(dto.getDepartment(), valFont));
        }
        if (dto.getSpecialization() != null) {
            docCell.addElement(spacerPara(6f));
            docCell.addElement(para("Specialization", labelFont));
            docCell.addElement(para(dto.getSpecialization(), valFont));
        }

        table.addCell(patCell);
        table.addCell(docCell);
        doc.add(table);
    }

    /** Three-column row: consultation type | appointment date | time slot. */
    private void addConsultationSection(Document doc, InvoiceReceiptDTO dto) throws DocumentException {
        sectionHeader(doc, "CONSULTATION DETAILS");

        Font labelFont = font(7.5f, Font.BOLD,   COL_LABEL);
        Font valFont   = font(9.5f, Font.NORMAL, COL_BODY);

        PdfPTable table = new PdfPTable(new float[]{1f, 1f, 1f});
        table.setWidthPercentage(100f);

        PdfPCell typeCell = borderedCell(COL_WHITE, false);
        typeCell.addElement(para("Consultation Type", labelFont));
        typeCell.addElement(para(fmtConsultationType(dto.getConsultationType()), valFont));

        PdfPCell dateCell = borderedCell(COL_WHITE, false);
        dateCell.addElement(para("Appointment Date", labelFont));
        dateCell.addElement(para(
                dto.getAppointmentDate() != null ? dto.getAppointmentDate().format(DATE_FMT) : "—",
                valFont));

        PdfPCell timeCell = borderedCell(COL_WHITE, false);
        timeCell.addElement(para("Time Slot", labelFont));
        timeCell.addElement(para(safe(dto.getAppointmentTime()), valFont));

        table.addCell(typeCell);
        table.addCell(dateCell);
        table.addCell(timeCell);
        doc.add(table);
    }

    /** Payment summary table with consultation fee, tax, discount, and highlighted total. */
    private void addPaymentSection(Document doc, InvoiceReceiptDTO dto) throws DocumentException {
        sectionHeader(doc, "PAYMENT SUMMARY");

        String cur = currencyPrefix(dto.getCurrencyCode());

        Font colHdrFont   = font(8f,   Font.BOLD,   COL_LABEL);
        Font lineFont     = font(9.5f, Font.NORMAL, COL_BODY);
        Font discountFont = font(9.5f, Font.NORMAL, COL_GREEN);
        Font totalLbl     = font(11f,  Font.BOLD,   COL_BODY);
        Font totalAmt     = font(13f,  Font.BOLD,   COL_INDIGO);
        Font statusFont   = font(10f,  Font.BOLD,   COL_GREEN);

        PdfPTable table = new PdfPTable(new float[]{3f, 1f});
        table.setWidthPercentage(100f);

        // Table column headers
        payRow(table, "Description", "Amount", colHdrFont, colHdrFont, COL_SECTION_BG, true);

        // Consultation Fee
        payRow(table, "Consultation Fee",
                fmtAmount(cur, dto.getConsultationFee()),
                lineFont, lineFont, COL_WHITE, false);

        // Tax (omit row if zero)
        if (dto.getTaxAmount() != null && dto.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            payRow(table, "Tax",
                    fmtAmount(cur, dto.getTaxAmount()),
                    lineFont, lineFont, COL_WHITE, false);
        }

        // Discount (omit row if zero)
        if (dto.getDiscountAmount() != null && dto.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            payRow(table, "Discount",
                    "- " + fmtAmount(cur, dto.getDiscountAmount()),
                    lineFont, discountFont, COL_WHITE, false);
        }

        // Total row (highlighted)
        PdfPCell totalLblCell = new PdfPCell();
        totalLblCell.setBackgroundColor(COL_INDIGO_BG);
        totalLblCell.setBorderColor(COL_BORDER);
        totalLblCell.setBorderWidthTop(0.5f);
        totalLblCell.setBorderWidthBottom(0.5f);
        totalLblCell.setBorderWidthLeft(0.5f);
        totalLblCell.setBorderWidthRight(0f);
        totalLblCell.setPadding(12f);
        totalLblCell.setPaddingLeft(14f);
        totalLblCell.addElement(new Paragraph("TOTAL PAID", totalLbl));

        PdfPCell totalAmtCell = new PdfPCell();
        totalAmtCell.setBackgroundColor(COL_INDIGO_BG);
        totalAmtCell.setBorderColor(COL_BORDER);
        totalAmtCell.setBorderWidthTop(0.5f);
        totalAmtCell.setBorderWidthBottom(0.5f);
        totalAmtCell.setBorderWidthLeft(0f);
        totalAmtCell.setBorderWidthRight(0.5f);
        totalAmtCell.setPadding(12f);
        totalAmtCell.setPaddingRight(14f);
        totalAmtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalAmtCell.addElement(new Paragraph(fmtAmount(cur, dto.getTotalAmount()), totalAmt));

        table.addCell(totalLblCell);
        table.addCell(totalAmtCell);
        doc.add(table);

        // Payment status + method below table
        spacer(doc, 8);
        PdfPTable statusRow = new PdfPTable(new float[]{1f, 1f});
        statusRow.setWidthPercentage(100f);

        Font sLbl = font(7.5f, Font.BOLD, COL_LABEL);

        PdfPCell stCell = new PdfPCell();
        stCell.setBorder(Rectangle.NO_BORDER);
        stCell.setPadding(4f);
        stCell.addElement(para("PAYMENT STATUS", sLbl));
        stCell.addElement(para("PAID", statusFont));
        statusRow.addCell(stCell);

        PdfPCell mthCell = new PdfPCell();
        mthCell.setBorder(Rectangle.NO_BORDER);
        mthCell.setPadding(4f);
        mthCell.addElement(para("PAYMENT METHOD", sLbl));
        mthCell.addElement(para(fmtPaymentMethod(dto.getPaymentMethod()),
                font(10f, Font.NORMAL, COL_BODY)));
        statusRow.addCell(mthCell);

        doc.add(statusRow);
    }

    /** Footer: separator line + disclaimer + audit line + support contact. */
    private void addFooter(Document doc, InvoiceReceiptDTO dto) throws DocumentException {
        LineSeparator sep = new LineSeparator(0.5f, 100f, COL_BORDER, Element.ALIGN_CENTER, 0f);
        doc.add(new Chunk(sep));
        spacer(doc, 8);

        Font footerFont = font(8f,  Font.ITALIC, COL_LABEL);
        Font smallFont  = font(7f,  Font.NORMAL, COL_MUTED);

        Paragraph line1 = new Paragraph(
                "This is a computer-generated invoice. No physical signature is required.", footerFont);
        line1.setAlignment(Element.ALIGN_CENTER);
        doc.add(line1);

        if (dto.getGeneratedBy() != null && dto.getGeneratedAt() != null) {
            Paragraph line2 = new Paragraph(
                    "Generated by " + dto.getGeneratedBy() +
                    " on " + dto.getGeneratedAt().format(DATETIME_FMT), smallFont);
            line2.setAlignment(Element.ALIGN_CENTER);
            line2.setSpacingBefore(3f);
            doc.add(line2);
        }

        if (dto.getSupportEmail() != null) {
            Paragraph line3 = new Paragraph(
                    "For billing queries, contact: " + dto.getSupportEmail(), smallFont);
            line3.setAlignment(Element.ALIGN_CENTER);
            line3.setSpacingBefore(2f);
            doc.add(line3);
        }

        if (dto.getHospitalCode() != null) {
            Paragraph line4 = new Paragraph(
                    safe(dto.getHospitalName()) + " — " + dto.getHospitalCode(), smallFont);
            line4.setAlignment(Element.ALIGN_CENTER);
            line4.setSpacingBefore(4f);
            doc.add(line4);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cell / table helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Adds a section header band (light grey background) spanning full width. */
    private void sectionHeader(Document doc, String title) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100f);

        Font f = font(8.5f, Font.BOLD, COL_NAVY);
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

    /** Label cell for the metadata grid (light background). */
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

    /** Value cell for the metadata grid (white background). */
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

    /** A detail/total cell with configurable border and background. */
    private PdfPCell borderedCell(Color bg, boolean allBorders) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setBorderColor(COL_BORDER);
        if (allBorders) {
            cell.setBorderWidth(0.5f);
        } else {
            cell.setBorderWidthTop(0.5f);
            cell.setBorderWidthBottom(0.5f);
            cell.setBorderWidthLeft(0.5f);
            cell.setBorderWidthRight(0.5f);
        }
        cell.setPadding(12f);
        return cell;
    }

    /** Adds a payment row (label | amount) to the payment summary table. */
    private void payRow(PdfPTable table, String label, String amount,
                        Font labelFont, Font amountFont, Color bg, boolean header) {
        PdfPCell lCell = new PdfPCell();
        lCell.setBackgroundColor(bg);
        lCell.setBorderColor(COL_BORDER);
        lCell.setBorderWidthBottom(header ? 0.5f : 0f);
        lCell.setBorderWidthTop(0f);
        lCell.setBorderWidthLeft(0.5f);
        lCell.setBorderWidthRight(0f);
        lCell.setPadding(10f);
        lCell.setPaddingLeft(14f);
        lCell.addElement(new Paragraph(label, labelFont));
        table.addCell(lCell);

        PdfPCell aCell = new PdfPCell();
        aCell.setBackgroundColor(bg);
        aCell.setBorderColor(COL_BORDER);
        aCell.setBorderWidthBottom(header ? 0.5f : 0f);
        aCell.setBorderWidthTop(0f);
        aCell.setBorderWidthLeft(0f);
        aCell.setBorderWidthRight(0.5f);
        aCell.setPadding(10f);
        aCell.setPaddingRight(14f);
        aCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        aCell.addElement(new Paragraph(amount, amountFont));
        table.addCell(aCell);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Formatting helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Creates a Helvetica font using OpenPDF 1.3.x int-constant API. */
    private Font font(float size, int style, Color color) {
        // Font(int family, float size, int style, Color color)
        // Font.HELVETICA = 0, Font.BOLD = 1, Font.ITALIC = 2, Font.NORMAL = 0
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

    private String buildAddressLine(InvoiceReceiptDTO dto) {
        StringBuilder sb = new StringBuilder();
        if (dto.getAddressLine1() != null) sb.append(dto.getAddressLine1());
        if (dto.getAddressLine2() != null && !dto.getAddressLine2().isBlank())
            sb.append(", ").append(dto.getAddressLine2());
        if (dto.getCity() != null)       sb.append(", ").append(dto.getCity());
        if (dto.getState() != null)      sb.append(", ").append(dto.getState());
        if (dto.getPostalCode() != null) sb.append(" ").append(dto.getPostalCode());
        if (dto.getCountry() != null)    sb.append(", ").append(dto.getCountry());
        return sb.toString();
    }

    private String buildContactLine(InvoiceReceiptDTO dto) {
        StringBuilder sb = new StringBuilder();
        if (dto.getHospitalPhone() != null) sb.append("Tel: ").append(dto.getHospitalPhone());
        if (dto.getHospitalEmail() != null) {
            if (sb.length() > 0) sb.append("   |   ");
            sb.append("Email: ").append(dto.getHospitalEmail());
        }
        return sb.toString();
    }

    private String currencyPrefix(String currencyCode) {
        return (currencyCode != null) ? currencyCode.toUpperCase() + " " : "INR ";
    }

    private String fmtAmount(String prefix, BigDecimal amount) {
        if (amount == null) return prefix + "0.00";
        return prefix + String.format("%,.2f", amount);
    }

    private String fmtPaymentMethod(String method) {
        if (method == null) return "—";
        return switch (method.toUpperCase()) {
            case "CASH"      -> "Cash";
            case "CARD"      -> "Credit / Debit Card";
            case "UPI"       -> "UPI";
            case "ONLINE"    -> "Online Transfer";
            case "INSURANCE" -> "Insurance";
            default          -> method;
        };
    }

    private String fmtConsultationType(String type) {
        if (type == null) return "—";
        return switch (type.toUpperCase()) {
            case "IN_PERSON" -> "In-Person";
            case "ONLINE"    -> "Online";
            case "FOLLOW_UP" -> "Follow-Up";
            default          -> type;
        };
    }
}
