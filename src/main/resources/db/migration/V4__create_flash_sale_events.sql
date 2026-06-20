CREATE TABLE flash_sale_events (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    product_id         BIGINT        NOT NULL,
    total_quantity     INT           NOT NULL,
    remaining_quantity INT           NOT NULL,
    sale_price         DECIMAL(10,2) NOT NULL,
    starts_at          DATETIME(6)   NOT NULL,
    ends_at            DATETIME(6)   NOT NULL,
    status             ENUM('SCHEDULED','ACTIVE','ENDED','CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
    created_at         DATETIME(6)   NOT NULL,
    updated_at         DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_flash_sale_events_product_id (product_id),
    KEY idx_flash_sale_events_status_starts_at (status, starts_at),
    CONSTRAINT fk_flash_sale_events_product
        FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;