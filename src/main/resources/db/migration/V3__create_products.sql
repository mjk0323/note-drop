CREATE TABLE products (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    coffee_bean_id BIGINT        NOT NULL,
    price          DECIMAL(10,2) NOT NULL,
    stock          INT           NOT NULL,
    is_flash_sale  TINYINT(1)    NOT NULL DEFAULT 0,
    status         ENUM('ACTIVE','SOLD_OUT','HIDDEN') NOT NULL DEFAULT 'ACTIVE',
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_products_status_flash_sale (status, is_flash_sale),
    CONSTRAINT fk_products_coffee_bean
        FOREIGN KEY (coffee_bean_id) REFERENCES coffee_beans (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
