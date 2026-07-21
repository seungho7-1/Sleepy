package com.sleepyproject.sleepy_backend.config;

import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.member.Role;
import com.sleepyproject.sleepy_backend.domain.product.Product;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import com.sleepyproject.sleepy_backend.repository.product.ProductRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class DummyDataInit {

    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            // MySQL에서 기존 컬럼들의 길이를 2000자로 늘려주기 위해 ALTER TABLE 실행
            jdbcTemplate.execute("ALTER TABLE product MODIFY image_url VARCHAR(2000)");
            jdbcTemplate.execute("ALTER TABLE product MODIFY purchase_url VARCHAR(2000)");
            jdbcTemplate.execute("ALTER TABLE product MODIFY video_url VARCHAR(2000)");
            jdbcTemplate.execute("ALTER TABLE product MODIFY description VARCHAR(2000)");
            
            // 상세 이미지 컬럼 추가
            try {
                jdbcTemplate.execute("ALTER TABLE product ADD COLUMN description_image_url VARCHAR(2000) NULL");
            } catch (Exception ex) {
                // 이미 존재하는 컬럼일 경우 예외 무시
            }
        } catch (Exception e) {
            // 이미 변경되었거나 에러 발생 시 무시하고 부팅 진행
        }

        if (productRepository.count() == 0) {
            
            Member seller1 = Member.builder()
                    .email("seller1@slime.com")
                    .password("1234")
                    .nickname("말랑공방")
                    .role(Role.SELLER)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            Member seller2 = Member.builder()
                    .email("seller2@slime.com")
                    .password("1234")
                    .nickname("푸른바다 슬라임")
                    .role(Role.SELLER)
                    .createdAt(LocalDateTime.now())
                    .build();

            Member admin = Member.builder()
                    .email("admin@slime.com")
                    .password("1234")
                    .nickname("관리자")
                    .role(Role.ADMIN)
                    .createdAt(LocalDateTime.now())
                    .build();

            memberRepository.saveAll(Arrays.asList(seller1, seller2, admin));

            productRepository.saveAll(Arrays.asList(
                    Product.builder()
                            .name("딸기우유 크런키 슬라임")
                            .price(12000)
                            .imageUrl("https://images.unsplash.com/photo-1594957685655-4675e2fa295c?q=80&w=300&auto=format&fit=crop")
                            .description("바스락거리는 소리가 매력적인 딸기향 크런키 슬라임입니다.")
                            .shopName("말랑공방")
                            .purchaseUrl("https://example.com/buy/1")
                            .seller(seller1)
                            .createdAt(LocalDateTime.now())
                            .category("SLIME")
                            .build(),
                    Product.builder()
                            .name("바다여행 클리어 슬라임")
                            .price(14000)
                            .imageUrl("https://images.unsplash.com/photo-1619864299946-b088e5d2dafe?q=80&w=300&auto=format&fit=crop")
                            .description("투명한 바다를 담은 영롱한 클리어 슬라임.")
                            .shopName("푸른바다 슬라임")
                            .purchaseUrl("https://example.com/buy/2")
                            .seller(seller2)
                            .createdAt(LocalDateTime.now())
                            .category("SLIME")
                            .build(),
                    Product.builder()
                            .name("레몬 샤베트 슬라임")
                            .price(11000)
                            .imageUrl("https://images.unsplash.com/photo-1631557008139-4ab64858852f?q=80&w=300&auto=format&fit=crop")
                            .description("사각사각 소리가 나는 레몬향 샤베트 슬라임!")
                            .shopName("말랑공방")
                            .purchaseUrl("https://example.com/buy/3")
                            .seller(seller1)
                            .createdAt(LocalDateTime.now())
                            .category("SLIME")
                            .build(),
                    Product.builder()
                            .name("우주 대폭발 크런키")
                            .price(13500)
                            .imageUrl("https://images.unsplash.com/photo-1549488344-1f9b8d2bd1f3?q=80&w=300&auto=format&fit=crop")
                            .description("블랙&퍼플 컬러의 강렬한 크런키 슬라임.")
                            .shopName("푸른바다 슬라임")
                            .purchaseUrl("https://example.com/buy/5")
                            .seller(seller2)
                            .createdAt(LocalDateTime.now())
                            .category("SLIME")
                            .build(),
                    Product.builder()
                            .name("말랑쫀득 물만두 말랑이")
                            .price(3500)
                            .imageUrl("https://images.unsplash.com/photo-1594957685655-4675e2fa295c?q=80&w=300&auto=format&fit=crop")
                            .description("손안에 쏙 들어오는 쫀득한 촉감의 물만두 말랑이입니다.")
                            .shopName("말랑공방")
                            .purchaseUrl("https://example.com/buy/6")
                            .seller(seller1)
                            .createdAt(LocalDateTime.now())
                            .category("MALLANGI")
                            .build(),
                    Product.builder()
                            .name("부드러운 식빵 스퀴시")
                            .price(6000)
                            .imageUrl("https://images.unsplash.com/photo-1619864299946-b088e5d2dafe?q=80&w=300&auto=format&fit=crop")
                            .description("천천히 복원되는 퐁신퐁신한 식빵 스퀴시.")
                            .shopName("푸른바다 슬라임")
                            .purchaseUrl("https://example.com/buy/7")
                            .seller(seller2)
                            .createdAt(LocalDateTime.now())
                            .category("SQUISHY")
                            .build(),
                    Product.builder()
                            .name("귀여운 슬랑이 DIY 촉감세트")
                            .price(15000)
                            .imageUrl("https://images.unsplash.com/photo-1631557008139-4ab64858852f?q=80&w=300&auto=format&fit=crop")
                            .description("나만의 슬랑이를 직접 만들 수 있는 DIY 촉감 놀이 세트.")
                            .shopName("말랑공방")
                            .purchaseUrl("https://example.com/buy/8")
                            .seller(seller1)
                            .createdAt(LocalDateTime.now())
                            .category("SLANGY")
                            .build()
            ));
        }
    }
}
