    CREATE TABLE users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    nickname   VARCHAR(100) NOT NULL,
    role       ENUM('GUEST','MEMBER','ADMIN') NOT NULL DEFAULT 'GUEST',
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY idx_users_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE taste_profiles (
    user_id           BIGINT       NOT NULL,
    acidity_score     DECIMAL(3,1) NOT NULL DEFAULT 0.0,
    bitterness_score  DECIMAL(3,1) NOT NULL DEFAULT 0.0,
    sweetness_score   DECIMAL(3,1) NOT NULL DEFAULT 0.0,
    body_score        DECIMAL(3,1) NOT NULL DEFAULT 0.0,
    aroma_score       DECIMAL(3,1) NOT NULL DEFAULT 0.0,
    preferred_origins JSON,
    review_count      INT          NOT NULL DEFAULT 0,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_taste_profiles_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
