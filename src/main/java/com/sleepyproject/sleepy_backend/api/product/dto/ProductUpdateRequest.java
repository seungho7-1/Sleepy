package com.sleepyproject.sleepy_backend.api.product.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
// 상품 정보 수정을 요청할 때 사용하는 DTO 클래스
public class ProductUpdateRequest {

    // 수정할 상품명
    private String name;

    // 수정할 가격
    private int price;

    // 수정할 상품 설명
    private String description;

    // 수정할 대표 이미지 URL 링크 주소
    private String imageUrl;

    // 수정할 슬라임 판매 마켓 스토어명
    private String shopName;

    // 수정할 외부 결제/구매 페이지 링크
    private String purchaseUrl;

    // 슬라임 부가 정보
    private Integer capacity; // 용량(ml)
    private String texture; // 질감
    private String scent; // 향
    private String color; // 색상
    
    private LocalDate releaseDate; // 출시일
    
    private List<String> tags; // 태그
}