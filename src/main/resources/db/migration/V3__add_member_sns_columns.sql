-- V3: Add missing columns to member table
ALTER TABLE member
    ADD COLUMN IF NOT EXISTS site_url       VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS introduction   TEXT         NULL,
    ADD COLUMN IF NOT EXISTS youtube_url    VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS instagram_url  VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS facebook_url   VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS tiktok_url     VARCHAR(255) NULL;
