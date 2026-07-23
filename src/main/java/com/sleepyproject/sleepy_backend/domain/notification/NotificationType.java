package com.sleepyproject.sleepy_backend.domain.notification;

public enum NotificationType {
    NEW_COMMENT,        // 내 게시글/댓글에 댓글/대댓글이 달림
    NEW_LIKE,           // 내 게시글/리뷰에 좋아요가 달림
    SYSTEM_ALERT,       // 시스템 전체 공지
    NEW_MESSAGE,        // 새로운 1:1 메시지 (추후 확장을 위함)
    WISHLIST_UPDATE,    // 찜한 판매자가 새 상품을 등록했을 때
    SELLER_APPROVAL,    // 판매자 권한 승인 알림
    SELLER_REJECTED,    // 판매자 권한 반려 알림
    NEW_REVIEW,         // 내 상품에 리뷰가 등록됨 (판매자용)
    NEW_SELLER_APPLICATION, // 새 판매자 신청 접수 (관리자용)
    NEW_REPORT,         // 새 신고 접수 (관리자용)
    NEW_INQUIRY,        // 새로운 1:1 문의 등록 (관리자용)
    INQUIRY_ANSWERED    // 내 1:1 문의에 답변이 달림 (사용자용)
}
