package com.sleepyproject.sleepy_backend.api.product.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
// API를 통해 클라이언트로 상품 정보를 응답할 때 사용하는 DTO 클래스
public class ProductResponse {
    // 상품 고유 ID
    private Long id;
    
    // 상품명
    private String name;
    
    // 상품 가격
    private int price;
    
    // 상품 설명
    private String description;

    // 상품 이미지 URL 주소
    private String imageUrl;

    // 슬라임 판매 마켓 스토어명
    private String shopName;

    // 결제 및 구매가 가능한 외부 스토어 주소 URL 링크
    private String purchaseUrl;

    // 이 상품을 등록한 판매자(Seller)의 회원 고유 ID
    // 프론트엔드에서 로그인한 유저의 ID와 비교하여 수정/삭제 버튼 노출 여부를 결정하는 데 사용됩니다.
    private Long sellerId;

    // 슬라임 부가 정보
    private Integer capacity; // 용량(ml)
    private String texture; // 질감
    private String scent; // 향
    private String color; // 색상
    
    private LocalDate releaseDate; // 출시일
    
    private List<String> tags; // 태그
}