-- V7: Fix notification table enum column name
-- In V6, 'type' was used instead of 'notification_type'. This drops the incorrect column and updates the correct one.
ALTER TABLE notification DROP COLUMN IF EXISTS type;
ALTER TABLE notification MODIFY COLUMN notification_type ENUM('NEW_COMMENT', 'NEW_LIKE', 'SYSTEM_ALERT', 'NEW_MESSAGE', 'WISHLIST_UPDATE', 'SELLER_APPROVAL', 'SELLER_REJECTED', 'NEW_REVIEW', 'NEW_SELLER_APPLICATION', 'NEW_REPORT', 'NEW_INQUIRY', 'INQUIRY_ANSWERED') NOT NULL;
