package com.mediflow.platform.auth.repository;

import com.mediflow.platform.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Used by UserDetailsServiceImpl and JWT filter to load user by login handle. */
    Optional<User> findByUsername(String username);

    /** Allows login by email address as an alternative to username. */
    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhoneNumber(String phoneNumber);
}
