package com.sleepyproject.sleepy_backend.domain.product;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 상품 고유 식별 번호 (PK, 데이터베이스에서 자동 증가)
    private Long id;

    // 상품명 (슬라임 이름 등)
    private String name;

    // 가격 (원화 단위)
    private int price;

    // 상품 이미지 저장 주소 (웹 URL 형식)
    private String imageUrl;

    // 상품 설명 (슬라임 느낌, 재질, 옵션 등 설명글)
    @Column(length = 1000)
    private String description;

    // 해당 상품을 판매하는 스토어/업체 이름 (예: "슬라임쿡", "와이영슬라임")
    private String shopName;

    // 실제 상품을 구매할 수 있는 외부 마켓/네이버스토어 등의 웹 결제 URL 링크
    private String purchaseUrl;

    // 슬라임 부가 정보
    private Integer capacity; // 용량(ml)
    private String texture; // 질감
    private String scent; // 향
    private String color; // 색상
    
    private LocalDate releaseDate; // 출시일

    // 상품 등록 시간
    private LocalDateTime createdAt;

    // 이 상품을 등록한 판매자 정보 (Member 엔티티와 N:1 다대일 지연 로딩 관계)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private Member seller;

    /**
     * 상품 정보 수정 메서드
     * 영속성 컨텍스트의 더티 체킹(Dirty Checking) 방식을 사용하여 DB 데이터를 업데이트함.
     *
     * @param name           수정할 상품명
     * @param price          수정할 가격
     * @param description    수정할 설명
     * @param imageUrl       수정할 대표 이미지 URL
     * @param shopName       수정할 스토어명
     * @param purchaseUrl    수정할 외부 구매 주소 URL
     * @param capacity       용량
     * @param texture        질감
     * @param scent          향
     * @param color          색상
     * @param releaseDate    출시일
     */
    public void update(String name, int price, String description, String imageUrl, String shopName, String purchaseUrl, 
                       Integer capacity, String texture, String scent, String color, LocalDate releaseDate) {
        this.name = name;
        this.price = price;
        this.description = description;
        this.imageUrl = imageUrl;
        this.shopName = shopName;
        this.purchaseUrl = purchaseUrl;
        this.capacity = capacity;
        this.texture = texture;
        this.scent = scent;
        this.color = color;
        this.releaseDate = releaseDate;
    }
}