CREATE TABLE orders (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    user_id             BIGINT        NOT NULL,
    product_id          BIGINT        NOT NULL,
    flash_sale_event_id BIGINT,
    quantity            INT           NOT NULL,
    total_price         DECIMAL(10,2) NOT NULL,
    status              ENUM('PENDING','CONFIRMED','CANCELLED','REFUNDED') NOT NULL DEFAULT 'PENDING',
    idempotency_key     VARCHAR(36)   NOT NULL,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY idx_orders_idempotency_key (idempotency_key),
    KEY idx_orders_user_id_created_at (user_id, created_at DESC),
    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_orders_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_orders_flash_sale_event
        FOREIGN KEY (flash_sale_event_id) REFERENCES flash_sale_events (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
