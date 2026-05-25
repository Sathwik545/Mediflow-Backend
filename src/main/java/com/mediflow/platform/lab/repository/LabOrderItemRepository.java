package com.mediflow.platform.lab.repository;

import com.mediflow.platform.lab.entity.LabOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabOrderItemRepository extends JpaRepository<LabOrderItem, Long> {

    List<LabOrderItem> findByLabOrder_LabOrderCode(String labOrderCode);

    boolean existsByLabOrder_LabOrderCodeAndTestName(String labOrderCode, String testName);
}
