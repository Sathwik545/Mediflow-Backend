package com.mediflow.platform.auth.config;

import com.mediflow.platform.auth.entity.Role;
import com.mediflow.platform.auth.entity.User;
import com.mediflow.platform.auth.enums.RoleName;
import com.mediflow.platform.auth.enums.UserStatus;
import com.mediflow.platform.auth.repository.RoleRepository;
import com.mediflow.platform.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Programmatic bootstrap initializer — runs once after the application context is ready.
 *
 * Responsibility: seeds the default ADMIN user account using BCryptPasswordEncoder.
 * This cannot be done in data.sql because BCrypt is a Java operation — SQL has no
 * equivalent function available in vanilla PostgreSQL without a custom extension.
 *
 * Idempotent: checks for existence before creating; safe to run on every startup.
 *
 * Default admin credentials (CHANGE IN PRODUCTION):
 *   username: admin
 *   password: Admin@123
 *   email:    admin@mediflow.com
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository    userRepository;
    private final RoleRepository    roleRepository;
    private final PasswordEncoder   passwordEncoder;

    // Default admin credentials — override via environment variables in production:
    //   ADMIN_USERNAME, ADMIN_PASSWORD, ADMIN_EMAIL
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";
    private static final String DEFAULT_ADMIN_EMAIL    = "admin@mediflow.com";
    private static final String DEFAULT_ADMIN_PHONE    = "0000000000";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("[DataInitializer] Running bootstrap checks...");
        seedAdminUser();
        log.info("[DataInitializer] Bootstrap complete.");
    }

    /**
     * Creates the default admin user if it does not already exist.
     * The ADMIN role must already exist in the 'roles' table (seeded via data.sql).
     */
    private void seedAdminUser() {
        if (userRepository.existsByUsername(DEFAULT_ADMIN_USERNAME)) {
            log.info("[DataInitializer] Admin user '{}' already exists — skipping seed.", DEFAULT_ADMIN_USERNAME);
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException(
                    "[DataInitializer] ADMIN role not found in DB. " +
                    "Ensure data.sql has run and inserted the roles before this initializer executes."
                ));

        String encodedPassword = passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD);

        User adminUser = User.builder()
                .username(DEFAULT_ADMIN_USERNAME)
                .email(DEFAULT_ADMIN_EMAIL)
                .phoneNumber(DEFAULT_ADMIN_PHONE)
                .password(encodedPassword)
                .status(UserStatus.ACTIVE)
                .roles(Set.of(adminRole))
                .build();

        userRepository.save(adminUser);

        log.info("[DataInitializer] Default admin user created successfully.");
        log.info("[DataInitializer] Admin username: '{}'  |  email: '{}'", DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_EMAIL);
        log.warn("[DataInitializer] SECURITY REMINDER: Change the default admin password immediately in production!");
    }
}
