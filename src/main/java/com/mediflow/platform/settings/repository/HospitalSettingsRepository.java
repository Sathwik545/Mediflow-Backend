package com.mediflow.platform.settings.repository;

import com.mediflow.platform.settings.entity.HospitalSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HospitalSettingsRepository extends JpaRepository<HospitalSettings, Long> {

    /**
     * Retrieves the single hospital settings row.
     * Spring Data JPA generates: SELECT ... LIMIT 1 ORDER BY id ASC.
     * Since the table is enforced to contain exactly one row, this always
     * returns the same record.
     */
    Optional<HospitalSettings> findTopByOrderByIdAsc();
}
