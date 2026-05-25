package com.mediflow.platform.lab.entity;

import com.mediflow.platform.common.audit.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "lab_order_items",
    indexes = {
        @Index(name = "idx_lab_order_item_order", columnList = "lab_order_id"),
        @Index(name = "idx_lab_order_item_code",  columnList = "test_code")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabOrderItem extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_order_id", nullable = false)
    private LabOrder labOrder;

    @Column(name = "test_code", length = 50)
    private String testCode;

    @Column(name = "test_name", nullable = false, length = 200)
    private String testName;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    // createdAt, updatedAt, createdBy, updatedBy — inherited from BaseAuditEntity
}
