package com.mediflow.platform.billing.controller;

import com.mediflow.platform.billing.dto.BillResponseDTO;
import com.mediflow.platform.billing.dto.PayBillRequestDTO;
import com.mediflow.platform.billing.service.BillService;
import com.mediflow.platform.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bills")
@RequiredArgsConstructor
@Tag(name = "Billing Management", description = "APIs for retrieving consultation bills and recording payments")
public class BillController {

    private final BillService billService;

    @Operation(
        summary = "List / search all bills (admin)",
        description = "Returns a paginated, server-side filtered list of all consultation bills. " +
                      "search matches billCode, appointmentCode, or patientCode (OR, case-insensitive). " +
                      "status filters by PENDING or PAID (paymentStatus) or CANCELLED (billStatus). " +
                      "Restricted to ADMIN role."
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<BillResponseDTO>>> getAllBills(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > 100) size = 100;
        Page<BillResponseDTO> bills = billService.getAllBills(search, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Bills retrieved successfully", bills));
    }

    @Operation(
        summary = "Get bill by code",
        description = "Retrieves the full details of a consultation bill using its unique code (e.g., BILL-2026-0001)."
    )
    @GetMapping("/{billCode}")
    public ResponseEntity<ApiResponse<BillResponseDTO>> getBillByCode(
            @PathVariable String billCode) {

        BillResponseDTO response = billService.getBillByCode(billCode);
        return ResponseEntity.ok(ApiResponse.success("Bill retrieved successfully", response));
    }

    @Operation(
        summary = "Get bills by patient",
        description = "Returns a paginated list of all consultation bills for a specific patient, " +
                      "sorted by generation date (newest first)."
    )
    @GetMapping("/patient/{patientCode}")
    public ResponseEntity<ApiResponse<Page<BillResponseDTO>>> getBillsByPatient(
            @PathVariable String patientCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (size > 100) size = 100;
        Page<BillResponseDTO> bills = billService.getBillsByPatient(patientCode, page, size);
        return ResponseEntity.ok(ApiResponse.success("Patient bills retrieved successfully", bills));
    }

    @Operation(
        summary = "Mark bill as paid",
        description = "Records a manual payment against a bill. " +
                      "Transitions bill status to PAID and appointment status to CONFIRMED. " +
                      "Requires a valid payment method (CASH, CARD, UPI, ONLINE, INSURANCE). " +
                      "Only PENDING bills linked to non-cancelled appointments can be paid."
    )
    @PutMapping("/{billCode}/pay")
    public ResponseEntity<ApiResponse<BillResponseDTO>> payBill(
            @PathVariable String billCode,
            @Valid @RequestBody PayBillRequestDTO request) {

        BillResponseDTO response = billService.payBill(billCode, request);
        return ResponseEntity.ok(ApiResponse.success("Payment recorded successfully. Appointment confirmed.", response));
    }
}
