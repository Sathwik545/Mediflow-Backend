package com.mediflow.platform.lab.controller;

import com.mediflow.platform.common.response.ApiResponse;
import com.mediflow.platform.lab.dto.LabOrderRequestDTO;
import com.mediflow.platform.lab.dto.LabOrderResponseDTO;
import com.mediflow.platform.lab.dto.LabOrderStatusUpdateDTO;
import com.mediflow.platform.lab.service.LabOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lab-orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Lab Orders", description = "Diagnostic test order management")
public class LabOrderController {

    private final LabOrderService labOrderService;

    @PostMapping
    @Operation(summary = "Create a lab order from a consultation",
               description = "ADMIN or DOCTOR. Patient and doctor are derived from the consultation — never supplied in the payload.")
    public ResponseEntity<ApiResponse<LabOrderResponseDTO>> createLabOrder(
            @Valid @RequestBody LabOrderRequestDTO request) {
        LabOrderResponseDTO result = labOrderService.createLabOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @GetMapping
    @Operation(summary = "List lab orders (paginated, role-filtered). Optional: search, status (ORDERED|SAMPLE_COLLECTED|IN_PROGRESS|COMPLETED|CANCELLED).")
    public ResponseEntity<ApiResponse<Page<LabOrderResponseDTO>>> getAllLabOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<LabOrderResponseDTO> result = labOrderService.getAllLabOrders(search, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{labOrderCode}")
    @Operation(summary = "Get lab order by code")
    public ResponseEntity<ApiResponse<LabOrderResponseDTO>> getLabOrderByCode(
            @PathVariable String labOrderCode) {
        LabOrderResponseDTO result = labOrderService.getLabOrderByCode(labOrderCode);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/patient/{patientCode}")
    @Operation(summary = "List lab orders for a patient (paginated)")
    public ResponseEntity<ApiResponse<Page<LabOrderResponseDTO>>> getLabOrdersByPatient(
            @PathVariable String patientCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<LabOrderResponseDTO> result = labOrderService.getLabOrdersByPatient(patientCode, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/doctor/{doctorCode}")
    @Operation(summary = "List lab orders for a doctor (paginated)")
    public ResponseEntity<ApiResponse<Page<LabOrderResponseDTO>>> getLabOrdersByDoctor(
            @PathVariable String doctorCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<LabOrderResponseDTO> result = labOrderService.getLabOrdersByDoctor(doctorCode, page, size);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/{labOrderCode}/status")
    @Operation(summary = "Update lab order status (ADMIN or DOCTOR)")
    public ResponseEntity<ApiResponse<LabOrderResponseDTO>> updateLabOrderStatus(
            @PathVariable String labOrderCode,
            @Valid @RequestBody LabOrderStatusUpdateDTO request) {
        LabOrderResponseDTO result = labOrderService.updateLabOrderStatus(labOrderCode, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
