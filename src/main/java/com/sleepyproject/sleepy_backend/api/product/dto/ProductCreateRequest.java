package com.sleepyproject.sleepy_backend.api.product.dto;

import lombok.Getter;
import java.time.LocalDate;
import java.util.List;

@Getter
// 상품 등록 요청을 전달받는 DTO 클래스
public class ProductCreateRequest {
    // 상품명
    private String name;
    
    // 상품 가격
    private int price;
    
    // 상품 설명
    private String description;
    
    // 상품 대표 이미지 URL 링크 주소
    private String imageUrl;

    // 슬라임 판매 마켓 스토어명
    private String shopName;

    // 실제 결제 및 구매가 진행되는 외부 마켓 링크 (네이버스토어 등)
    private String purchaseUrl;

    // 슬라임 부가 정보
    private Integer capacity; // 용량(ml)
    private String texture; // 질감
    private String scent; // 향
    private String color; // 색상
    
    private LocalDate releaseDate; // 출시일
    
    private List<String> tags; // 태그
}