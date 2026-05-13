package com.mediflow.platform.dashboard.controller;

import com.mediflow.platform.common.response.ApiResponse;
import com.mediflow.platform.dashboard.dto.DashboardStatsDTO;
import com.mediflow.platform.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "APIs for dashboard KPI statistics")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Get dashboard stats", description = "Returns total patients, active doctors, today's appointments, and monthly revenue")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getStats() {
        return ResponseEntity.ok(
                ApiResponse.success("Dashboard stats retrieved successfully", dashboardService.getStats())
        );
    }
}
