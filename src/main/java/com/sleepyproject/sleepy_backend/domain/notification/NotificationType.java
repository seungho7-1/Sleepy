package com.sleepyproject.sleepy_backend.domain.notification;

public enum NotificationType {
    NEW_COMMENT,    // 내 게시글에 댓글이 달림
    NEW_LIKE,       // 내 게시글/리뷰에 좋아요가 달림
    SYSTEM_ALERT,   // 시스템 전체 공지
    NEW_MESSAGE,    // 새로운 1:1 메시지 (추후 확장을 위함)
    WISHLIST_UPDATE, // 관심 상품(찜) 업데이트 알림
    SELLER_APPROVAL // 판매자 권한 승인 알림
}
