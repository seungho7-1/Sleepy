package com.sleepyproject.sleepy_backend.service.board;

import com.sleepyproject.sleepy_backend.api.board.dto.PostResponse;
import com.sleepyproject.sleepy_backend.domain.board.BoardType;
import com.sleepyproject.sleepy_backend.domain.board.Post;
import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.repository.board.PostRepository;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@Transactional
class BoardServiceTest {

    @Autowired
    private BoardService boardService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostRepository postRepository;


    @BeforeEach
    void insertTestData() {

        Member member = memberRepository.findByUsername("kshdeco")
                .orElseThrow();


        List<Post> posts = new ArrayList<>();

        for (int i = 0; i < 100000; i++) {

            Post post = Post.builder()
                    .member(member)
                    .title("테스트 게시글 " + i)
                    .content("테스트 내용입니다 " + i)
                    .boardType(BoardType.FREE)
                    .createdAt(LocalDateTime.now())
                    .build();

            posts.add(post);


            // 1000개씩 저장
            if (posts.size() == 1000) {
                postRepository.saveAll(posts);
                posts.clear();
            }
        }

        if (!posts.isEmpty()) {
            postRepository.saveAll(posts);
        }
    }


    @Test
    void 게시글조회_성능테스트() {


        long start = System.currentTimeMillis();


        Page<PostResponse> result =
                boardService.getPosts(
                        "FREE",
                        null,
                        PageRequest.of(0,20),
                        null
                );


        long end = System.currentTimeMillis();


        System.out.println(
                "조회 시간 : "
                        + (end-start)
                        + "ms"
        );


        System.out.println(
                "조회 개수 : "
                        + result.getContent().size()
        );
    }
}