package com.mediflow.platform.auth.entity;

import com.mediflow.platform.auth.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity for the 'roles' table.
 *
 * Roles are system-level constants (ADMIN, DOCTOR, PATIENT) seeded via data.sql.
 * They are never created at runtime — the set is fixed at deploy time.
 *
 * The name column uses the RoleName enum stored as STRING (not ordinal)
 * so column values are human-readable and survive enum reordering.
 */
@Entity
@Table(
    name = "roles",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_role_name", columnNames = "name")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** System role identifier. Stored as enum string name (e.g., "ADMIN"). */
    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 20)
    private RoleName name;
}
