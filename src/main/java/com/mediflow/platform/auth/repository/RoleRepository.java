package com.mediflow.platform.auth.repository;

import com.mediflow.platform.auth.entity.Role;
import com.mediflow.platform.auth.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /** Find a role by its canonical name (e.g., ADMIN, DOCTOR, PATIENT). */
    Optional<Role> findByName(RoleName name);

    boolean existsByName(RoleName name);
}
