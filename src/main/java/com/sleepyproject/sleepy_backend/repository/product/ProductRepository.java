package com.sleepyproject.sleepy_backend.repository.product;

import com.sleepyproject.sleepy_backend.domain.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"seller"})
    Page<Product> findAll(Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"seller"})
    Page<Product> findByNameContaining(String keyword, Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"seller"})
    Page<Product> findByNameContainingOrShopNameContaining(String nameKeyword, String shopNameKeyword, Pageable pageable);

    // 특정 판매자(seller)가 등록한 상품 목록을 최신순으로 조회 (마이페이지 내 상품 목록용)
    List<Product> findBySellerIdOrderByIdDesc(Long sellerId);
    long countByCreatedAtAfter(java.time.LocalDateTime date);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"seller"})
    @org.springframework.data.jpa.repository.Query("select p from Product p where " +
            "(:sellerId is null or p.seller.id = :sellerId) and " +
            "(:category is null or :category = '' or p.category = :category) and " +
            "(:keyword is null or :keyword = '' or p.name like %:keyword% or p.shopName like %:keyword%)")
    Page<Product> findByCategoryAndKeywordAndSellerId(String category, String keyword, Long sellerId, Pageable pageable);
}
