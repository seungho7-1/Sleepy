package com.sleepyproject.sleepy_backend.repository.product;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.product.Product;
import com.sleepyproject.sleepy_backend.domain.product.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    Optional<Wishlist> findByMemberAndProduct(Member member, Product product);
    List<Wishlist> findByMember(Member member);
    void deleteByProduct(Product product);

    /** 특정 판매자의 상품 중 하나라도 찜한 회원 목록 (중복 포함) */
    List<Wishlist> findByProductSellerId(Long sellerId);
}
