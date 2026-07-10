package com.sleepyproject.sleepy_backend.domain.product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품과 해시태그 간의 다대다(N:M) 연관관계를 해소하기 위한 중간 매핑 엔티티 클래스입니다.
 */
@Entity
@Getter
@NoArgsConstructor
public class ProductTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public ProductTag(Product product, Tag tag) {
        this.product = product;
        this.tag = tag;
    }
}
