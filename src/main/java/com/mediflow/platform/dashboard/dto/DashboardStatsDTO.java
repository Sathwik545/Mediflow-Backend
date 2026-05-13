package com.mediflow.platform.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {

    private long totalPatients;
    private long activeDoctors;
    private long todayAppointments;
    private long monthlyRevenue;

    @Builder.Default
    private double patientTrend = 0.0;
    @Builder.Default
    private double doctorTrend = 0.0;
    @Builder.Default
    private double appointmentTrend = 0.0;
    @Builder.Default
    private double revenueTrend = 0.0;

    private String patientSub;
    private String doctorSub;
    private String appointmentSub;
    private String revenueSub;
}
