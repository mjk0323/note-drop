CREATE TABLE cafes (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL,
    address         VARCHAR(255) NOT NULL,
    operating_hours VARCHAR(255) NOT NULL,
    map_url         VARCHAR(500) NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_cafes_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE cafe_coffee_beans (
    cafe_id        BIGINT NOT NULL,
    coffee_bean_id BIGINT NOT NULL,
    PRIMARY KEY (cafe_id, coffee_bean_id),
    CONSTRAINT fk_cafe_coffee_beans_cafe
        FOREIGN KEY (cafe_id) REFERENCES cafes (id) ON DELETE CASCADE,
    CONSTRAINT fk_cafe_coffee_beans_coffee_bean
        FOREIGN KEY (coffee_bean_id) REFERENCES coffee_beans (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;