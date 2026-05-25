package com.mediflow.platform.lab.service;

import com.mediflow.platform.lab.dto.LabOrderRequestDTO;
import com.mediflow.platform.lab.dto.LabOrderResponseDTO;
import com.mediflow.platform.lab.dto.LabOrderStatusUpdateDTO;
import org.springframework.data.domain.Page;

public interface LabOrderService {

    LabOrderResponseDTO createLabOrder(LabOrderRequestDTO request);

    LabOrderResponseDTO getLabOrderByCode(String labOrderCode);

    Page<LabOrderResponseDTO> getAllLabOrders(String search, String status, int page, int size);

    Page<LabOrderResponseDTO> getLabOrdersByPatient(String patientCode, int page, int size);

    Page<LabOrderResponseDTO> getLabOrdersByDoctor(String doctorCode, int page, int size);

    LabOrderResponseDTO updateLabOrderStatus(String labOrderCode, LabOrderStatusUpdateDTO request);
}
