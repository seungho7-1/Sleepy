-- V3: Add missing columns to member table (MySQL 8.0 compatible)
ALTER TABLE member
    ADD COLUMN site_url       VARCHAR(255) NULL,
    ADD COLUMN introduction   TEXT         NULL,
    ADD COLUMN youtube_url    VARCHAR(255) NULL,
    ADD COLUMN instagram_url  VARCHAR(255) NULL,
    ADD COLUMN facebook_url   VARCHAR(255) NULL,
    ADD COLUMN tiktok_url     VARCHAR(255) NULL;
