package com.sleepyproject.sleepy_backend.api.product.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
// 상품 정보 수정을 요청할 때 사용하는 DTO 클래스
public class ProductUpdateRequest {

    // 수정할 상품명
    private String name;

    // 수정할 가격
    private int price;

    // 수정할 상품 설명
    private String description;

    // 수정할 이미지 URL 링크 주소 목록 (최대 5개)
    private List<String> imageUrls;

    // 수정할 슬라임 판매 마켓 스토어명
    private String shopName;

    // 수정할 외부 결제/구매 페이지 링크
    private String purchaseUrl;

    // 슬라임 부가 정보

    private String texture; // 질감
    private String scent; // 향
    private String color; // 색상
    
    private LocalDate releaseDate; // 출시일
    
    private List<String> tags; // 태그

    private String category; // 카테고리 (SLIME, SLANGY, MALLANGI, SQUISHY)

    private String videoUrl; // 비디오 주소
    private String videoType; // 비디오 타입 (FILE, LINK, NONE)
    private List<String> descriptionImageUrls; // 상세 설명 이미지 URL 목록
}