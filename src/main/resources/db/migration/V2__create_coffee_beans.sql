CREATE TABLE coffee_beans (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    name              VARCHAR(100) NOT NULL,
    description       TEXT,
    origin            VARCHAR(100) NOT NULL,
    processing_method ENUM('WASHED','NATURAL','HONEY','ANAEROBIC') NOT NULL,
    roast_level       ENUM('LIGHT','MEDIUM_LIGHT','MEDIUM','MEDIUM_DARK','DARK') NOT NULL,
    acidity_score     DECIMAL(3,1) NOT NULL,
    bitterness_score  DECIMAL(3,1) NOT NULL,
    sweetness_score   DECIMAL(3,1) NOT NULL,
    body_score        DECIMAL(3,1) NOT NULL,
    aroma_score       DECIMAL(3,1) NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_coffee_beans_origin (origin),
    KEY idx_coffee_beans_roast_level (roast_level),
    KEY idx_coffee_beans_taste_scores (acidity_score, sweetness_score)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
