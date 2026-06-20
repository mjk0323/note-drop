ALTER TABLE taste_profiles
    ADD COLUMN user_level          ENUM('BEGINNER', 'ENTHUSIAST') NULL,
    ADD COLUMN onboarding_complete TINYINT(1) NOT NULL DEFAULT 0;