-- V2: Add brand_scrap table
CREATE TABLE IF NOT EXISTS brand_scrap (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    member_id  BIGINT       NOT NULL,
    seller_id  BIGINT       NOT NULL,
    created_at DATETIME(6)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_brand_scrap_member_seller (member_id, seller_id),
    CONSTRAINT fk_brand_scrap_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_brand_scrap_seller FOREIGN KEY (seller_id) REFERENCES member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
