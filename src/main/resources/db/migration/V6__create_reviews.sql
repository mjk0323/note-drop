CREATE TABLE reviews (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    product_id        BIGINT       NOT NULL,
    coffee_bean_id    BIGINT       NOT NULL,
    acidity_rating    DECIMAL(3,1) NOT NULL,
    bitterness_rating DECIMAL(3,1) NOT NULL,
    sweetness_rating  DECIMAL(3,1) NOT NULL,
    body_rating       DECIMAL(3,1) NOT NULL,
    aroma_rating      DECIMAL(3,1) NOT NULL,
    overall_rating    DECIMAL(3,1) NOT NULL,
    content           TEXT,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY idx_reviews_user_product (user_id, product_id),
    CONSTRAINT fk_reviews_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_reviews_coffee_bean
        FOREIGN KEY (coffee_bean_id) REFERENCES coffee_beans (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
