-- V7: Fix notification table enum column name
-- Rename 'type' to 'notification_type' and update the enum values
ALTER TABLE notification CHANGE COLUMN type notification_type ENUM('NEW_COMMENT', 'NEW_LIKE', 'SYSTEM_ALERT', 'NEW_MESSAGE', 'WISHLIST_UPDATE', 'SELLER_APPROVAL', 'SELLER_REJECTED', 'NEW_REVIEW', 'NEW_SELLER_APPLICATION', 'NEW_REPORT', 'NEW_INQUIRY', 'INQUIRY_ANSWERED') NOT NULL;
