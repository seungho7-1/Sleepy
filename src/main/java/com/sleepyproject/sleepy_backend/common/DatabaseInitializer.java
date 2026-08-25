package com.sleepyproject.sleepy_backend.common;

import com.sleepyproject.sleepy_backend.domain.board.BoardType;
import com.sleepyproject.sleepy_backend.domain.board.Post;
import com.sleepyproject.sleepy_backend.domain.board.Comment;
import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.member.Role;
import com.sleepyproject.sleepy_backend.domain.product.Product;
import com.sleepyproject.sleepy_backend.domain.review.Review;
import com.sleepyproject.sleepy_backend.repository.board.CommentRepository;
import com.sleepyproject.sleepy_backend.repository.board.PostRepository;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import com.sleepyproject.sleepy_backend.repository.product.ProductRepository;
import com.sleepyproject.sleepy_backend.repository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create default accounts and seed data if admin does not exist
        if (memberRepository.findByEmail("admin@sleepy.com").isPresent()) {
            return;
        }
        log.info("Creating default accounts and seed data...");
            
        // Admin
        Member admin = Member.builder()
                .username("admin_dummy")
                    .email("admin@sleepy.com")
                    .password(passwordEncoder.encode("admin1234"))
                    .nickname("관리자")
                    .role(Role.ADMIN)
                    .createdAt(LocalDateTime.now())
                    .onboarded(true)
                    .build();
            memberRepository.save(admin);

            // Seller
            Member seller = Member.builder()
                    .username("seller1")
                    .email("seller1@sleepy.com")
                    .password(passwordEncoder.encode("seller1234"))
                    .nickname("슬라임팩토리")
                    .role(Role.SELLER)
                    .createdAt(LocalDateTime.now())
                    .onboarded(true)
                    .build();
            memberRepository.save(seller);

            // Buyer
            Member buyer = Member.builder()
                    .username("buyer1")
                    .email("buyer1@sleepy.com")
                    .password(passwordEncoder.encode("buyer1234"))
                    .nickname("슬라임러버")
                    .role(Role.BUYER)
                    .createdAt(LocalDateTime.now())
                    .onboarded(true)
                    .build();
            memberRepository.save(buyer);

            // 2. Create products (registered by seller)
            Product product1 = Product.builder()
                    .name("블루베리 요거트 슬라임")
                    .price(13500)
                    .imageUrl("https://images.unsplash.com/photo-1555529669-e69e7aa0ba9a?w=800")
                    .description("달콤한 블루베리 향이 가득한 요거트 질감의 버터 슬라임입니다. 부드러운 플레잉이 가능하여 입문자분들께 추천드려요!")
                    .shopName("슬라임팩토리")
                    .purchaseUrl("https://smartstore.naver.com")
                    .texture("버터")
                    .scent("블루베리")
                    .color("보라색")
                    .releaseDate(LocalDate.now())
                    .createdAt(LocalDateTime.now())
                    .seller(seller)
                    .build();
            productRepository.save(product1);

            Product product2 = Product.builder()
                    .name("크리스탈 클리어 슬라임")
                    .price(11000)
                    .imageUrl("https://images.unsplash.com/photo-1549490349-8643362247b5?w=800")
                    .description("유리알처럼 맑고 투명한 오리지널 클리어 슬라임입니다. 뽀득뽀득 기포 소리가 최고예요!")
                    .shopName("슬라임팩토리")
                    .purchaseUrl("https://smartstore.naver.com")
                    .texture("클리어")
                    .scent("무향")
                    .color("투명")
                    .releaseDate(LocalDate.now())
                    .createdAt(LocalDateTime.now())
                    .seller(seller)
                    .build();
            productRepository.save(product2);

            // 3. Create reviews (written by buyer)
            Review review1 = Review.builder()
                    .product(product1)
                    .member(buyer)
                    .rating(5)
                    .content("향도 너무 좋고 진짜 부드러워요! 대만족입니다.")
                    .imageUrl("")
                    .createdAt(LocalDateTime.now())
                    .build();
            reviewRepository.save(review1);

            // 4. Create board posts
            Post post1 = Post.builder()
                    .member(buyer)
                    .title("슬라임 관리 꿀팁 공유합니다!")
                    .content("슬라임이 너무 녹았을 때는 액티베이터를 한두 방울씩 섞어주면 다시 쫄깃해져요! 반대로 너무 단단해졌을 때는 글리세린을 섞거나 따뜻한 곳에 며칠 보관해 보세요.")
                    .boardType(BoardType.FREE)
                    .imageUrl("")
                    .createdAt(LocalDateTime.now())
                    .build();
            postRepository.save(post1);

            Post post2 = Post.builder()
                    .member(buyer)
                    .title("버터 슬라임이랑 클리어 슬라임 중 뭐가 더 좋나요?")
                    .content("슬라임 입문한 지 얼마 안 된 뉴비입니다! 버터 질감이랑 클리어 질감 중에 어떤 걸 먼저 구매하는 게 좋을까요? 추천 부탁드립니다!")
                    .boardType(BoardType.QNA)
                    .imageUrl("")
                    .createdAt(LocalDateTime.now())
                    .build();
            postRepository.save(post2);

            Post post3 = Post.builder()
                    .member(admin)
                    .title("[공지] 슬라임 마켓 플랫폼 슬리피 그랜드 오픈!")
                    .content("슬라임 덕후들을 위한 최고의 마켓 플랫폼 슬리피가 정식 오픈했습니다! 다양한 마켓의 예쁜 슬라임들을 둘러보고 이야기를 나눠보세요.")
                    .boardType(BoardType.NOTICE)
                    .imageUrl("")
                    .createdAt(LocalDateTime.now())
                    .build();
            postRepository.save(post3);

            // 5. Create comments
            Comment comment1 = Comment.builder()
                    .member(seller)
                    .post(post2)
                    .content("처음이시라면 손붙음이 적고 부드러운 버터 슬라임을 추천해 드립니다! 어느 정도 익숙해지시면 소리가 잘 나는 클리어 슬라임으로 넘어가 보세요.")
                    .createdAt(LocalDateTime.now())
                    .build();
            commentRepository.save(comment1);

            log.info("Default seed data provisioned successfully!");
    }
}
