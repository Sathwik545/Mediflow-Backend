package com.mediflow.platform.lab.mapper;

import com.mediflow.platform.lab.dto.LabOrderItemResponseDTO;
import com.mediflow.platform.lab.dto.LabOrderResponseDTO;
import com.mediflow.platform.lab.entity.LabOrder;
import com.mediflow.platform.lab.entity.LabOrderItem;
import com.mediflow.platform.lab.entity.LabReport;
import com.mediflow.platform.lab.repository.LabReportRepository;

import java.util.List;
import java.util.Optional;

public class LabOrderMapper {

    public static LabOrderResponseDTO toResponseDTO(LabOrder order, LabReportRepository labReportRepository) {
        List<LabOrderItemResponseDTO> itemDTOs = order.getItems().stream()
                .map(item -> toItemResponseDTO(item, labReportRepository))
                .toList();

        return LabOrderResponseDTO.builder()
                .labOrderCode(order.getLabOrderCode())
                .orderDate(order.getOrderDate())
                .priority(order.getPriority().name())
                .status(order.getStatus().name())
                .consultationCode(order.getConsultation().getConsultationCode())
                .appointmentCode(order.getAppointment().getAppointmentCode())
                .patientCode(order.getPatient().getPatientCode())
                .patientName(order.getPatient().getFirstName() + " " + order.getPatient().getLastName())
                .doctorCode(order.getDoctor().getDoctorCode())
                .doctorName(order.getDoctor().getFirstName() + " " + order.getDoctor().getLastName())
                .clinicalNotes(order.getClinicalNotes())
                .instructions(order.getInstructions())
                .items(itemDTOs)
                .testsCount(itemDTOs.size())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .createdBy(order.getCreatedBy())
                .updatedBy(order.getUpdatedBy())
                .build();
    }

    public static LabOrderItemResponseDTO toItemResponseDTO(LabOrderItem item,
                                                             LabReportRepository labReportRepository) {
        Optional<LabReport> report = labReportRepository.findByLabOrderItem_Id(item.getId());

        return LabOrderItemResponseDTO.builder()
                .id(item.getId())
                .testCode(item.getTestCode())
                .testName(item.getTestName())
                .category(item.getCategory())
                .remarks(item.getRemarks())
                .hasReport(report.isPresent())
                .reportCode(report.map(LabReport::getReportCode).orElse(null))
                .reportStatus(report.map(r -> r.getReportStatus().name()).orElse(null))
                .build();
    }
}
