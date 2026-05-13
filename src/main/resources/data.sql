-- =============================================================================
-- MediFlow Platform — Bootstrap Seed Data
-- =============================================================================
-- Runs on every application startup via spring.sql.init.mode=always.
-- All statements use ON CONFLICT DO NOTHING to remain idempotent.
-- The admin USER is seeded programmatically by DataInitializer.java (BCrypt).
-- =============================================================================

-- Seed roles (fixed system roles — never modified at runtime)
INSERT INTO roles (name) VALUES ('ADMIN')   ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('DOCTOR')  ON CONFLICT (name) DO NOTHING;
INSERT INTO roles (name) VALUES ('PATIENT') ON CONFLICT (name) DO NOTHING;
