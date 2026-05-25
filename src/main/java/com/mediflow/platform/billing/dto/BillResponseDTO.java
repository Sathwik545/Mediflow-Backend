package com.mediflow.platform.billing.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.mediflow.platform.billing.enums.BillStatus;
import com.mediflow.platform.billing.enums.BillType;
import com.mediflow.platform.billing.enums.PaymentMethod;
import com.mediflow.platform.billing.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillResponseDTO {

    private String billCode;
    private String appointmentCode;

    private String patientCode;
    private String patientName;

    private String doctorCode;
    private String doctorName;

    private BillType billType;

    private BigDecimal consultationFee;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private BillStatus billStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime generatedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Audit identity — populated from JWT SecurityContext, never from client payload
    private String createdBy;
    private String updatedBy;
}
