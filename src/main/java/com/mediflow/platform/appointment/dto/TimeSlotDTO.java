package com.mediflow.platform.appointment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotDTO {

    /** Start time in HH:mm format (e.g. "09:00"). */
    private String startTime;

    /** End time in HH:mm format (e.g. "09:30"). */
    private String endTime;

    /** False when an active (non-cancelled, non-no-show) appointment already occupies this slot. */
    private boolean available;
}
