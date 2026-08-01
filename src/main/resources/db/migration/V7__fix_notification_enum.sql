-- V7: Fix notification table enum column name
-- Update the enum values for existing notification_type column
ALTER TABLE notification MODIFY COLUMN notification_type ENUM('NEW_COMMENT', 'NEW_LIKE', 'SYSTEM_ALERT', 'NEW_MESSAGE', 'WISHLIST_UPDATE', 'SELLER_APPROVAL', 'SELLER_REJECTED', 'NEW_REVIEW', 'NEW_SELLER_APPLICATION', 'NEW_REPORT', 'NEW_INQUIRY', 'INQUIRY_ANSWERED') NOT NULL;
