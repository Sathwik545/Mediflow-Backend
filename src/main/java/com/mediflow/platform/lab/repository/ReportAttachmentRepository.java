package com.mediflow.platform.lab.repository;

import com.mediflow.platform.lab.entity.ReportAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportAttachmentRepository extends JpaRepository<ReportAttachment, Long> {

    List<ReportAttachment> findByLabReport_ReportCode(String reportCode);
}
