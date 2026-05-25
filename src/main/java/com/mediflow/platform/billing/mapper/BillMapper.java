package com.mediflow.platform.billing.mapper;

import com.mediflow.platform.billing.dto.BillResponseDTO;
import com.mediflow.platform.billing.entity.Bill;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BillMapper {

    /**
     * Converts a Bill entity to the outbound response DTO.
     * Patient and doctor names are read from immutable snapshot fields frozen at bill generation time,
     * ensuring invoice accuracy even if names change after the fact.
     * The consultation fee is served from the immutable consultationFeeSnapshot field.
     */
    public static BillResponseDTO toResponseDTO(Bill bill) {
        return BillResponseDTO.builder()
                .billCode(bill.getBillCode())
                .appointmentCode(bill.getAppointment().getAppointmentCode())
                .patientCode(bill.getPatient().getPatientCode())
                .patientName(bill.getPatientNameSnapshot())
                .doctorCode(bill.getDoctor().getDoctorCode())
                .doctorName(bill.getDoctorNameSnapshot())
                .billType(bill.getBillType())
                .consultationFee(bill.getConsultationFeeSnapshot())
                .taxAmount(bill.getTaxAmount())
                .discountAmount(bill.getDiscountAmount())
                .totalAmount(bill.getTotalAmount())
                .paymentStatus(bill.getPaymentStatus())
                .paymentMethod(bill.getPaymentMethod())
                .billStatus(bill.getBillStatus())
                .generatedAt(bill.getGeneratedAt())
                .paidAt(bill.getPaidAt())
                .createdAt(bill.getCreatedAt())
                .updatedAt(bill.getUpdatedAt())
                .createdBy(bill.getCreatedBy())
                .updatedBy(bill.getUpdatedBy())
                .build();
    }
}
