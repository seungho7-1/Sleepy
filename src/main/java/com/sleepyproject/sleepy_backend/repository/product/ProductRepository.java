package com.sleepyproject.sleepy_backend.repository.product;

import com.sleepyproject.sleepy_backend.domain.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByNameContaining(String keyword, Pageable pageable);
    Page<Product> findByNameContainingOrShopNameContaining(String nameKeyword, String shopNameKeyword, Pageable pageable);

    // 특정 판매자(seller)가 등록한 상품 목록을 최신순으로 조회 (마이페이지 내 상품 목록용)
    List<Product> findBySellerIdOrderByIdDesc(Long sellerId);
    long countByCreatedAtAfter(java.time.LocalDateTime date);
}
