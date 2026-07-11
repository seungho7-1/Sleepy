package com.sleepyproject.sleepy_backend.service.board;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import com.sleepyproject.sleepy_backend.api.board.dto.CommentRequest;
import com.sleepyproject.sleepy_backend.api.board.dto.CommentResponse;
import com.sleepyproject.sleepy_backend.api.board.dto.PostRequest;
import com.sleepyproject.sleepy_backend.api.board.dto.PostResponse;
import com.sleepyproject.sleepy_backend.api.board.dto.MyCommentResponse;
import com.sleepyproject.sleepy_backend.domain.board.BoardType;
import com.sleepyproject.sleepy_backend.domain.board.Comment;
import com.sleepyproject.sleepy_backend.domain.board.Post;
import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.review.Review;
import com.sleepyproject.sleepy_backend.repository.board.CommentRepository;
import com.sleepyproject.sleepy_backend.repository.board.PostRepository;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import com.sleepyproject.sleepy_backend.repository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 커뮤니티 게시판 및 댓글 처리를 담당하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
public class BoardService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;

    /**
     * 커뮤니티 게시글 생성 로직
     *
     * @param request 게시글 생성 요청 DTO (제목, 내용, 게시판 타입)
     * @param email   요청한 유저의 이메일
     * @return 생성된 게시글의 ID
     */
    @Transactional
    public Long createPost(PostRequest request, String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Post post = Post.builder()
                .member(member)
                .title(request.getTitle())
                .content(request.getContent())
                .boardType(BoardType.valueOf(request.getBoardType().toUpperCase()))
                .imageUrl(request.getImageUrl())
                .createdAt(LocalDateTime.now())
                .build();

        return postRepository.save(post).getId();
    }

    /**
     * 게시판 카테고리별 게시글 목록 페이징 조회
     *
     * @param boardTypeStr 게시판 타입 문자열 (FREE, QNA, NOTICE)
     * @param pageable     페이징 정보
     * @return 페이징 처리된 게시글 목록 (PostResponse 형태)
     */
    @Transactional(readOnly = true)
    public Page<PostResponse> getPosts(String boardTypeStr, Pageable pageable) {
        BoardType type = BoardType.valueOf(boardTypeStr.toUpperCase());
        return postRepository.findByBoardType(type, pageable).map(p -> new PostResponse(
                p.getId(), p.getTitle(), p.getContent(), p.getBoardType().name(), p.getImageUrl(),
                p.getMember().getNickname(), p.getViewCount(), p.getLikeCount(), p.getCreatedAt()
        ));
    }

    /**
     * 특정 게시글 상세 조회 및 조회수 증가 로직
     *
     * @param postId 조회할 게시글 ID
     * @return 게시글 상세 정보 (PostResponse)
     */
    @Transactional
    public PostResponse getPostDetail(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        post.incrementViewCount(); // 조회수 증가
        return new PostResponse(
                post.getId(), post.getTitle(), post.getContent(), post.getBoardType().name(), post.getImageUrl(),
                post.getMember().getNickname(), post.getViewCount(), post.getLikeCount(), post.getCreatedAt()
        );
    }

    /**
     * 댓글 생성 로직 (게시글 또는 리뷰 대상)
     *
     * @param request 댓글 생성 요청 DTO (타겟 타입, 타겟 ID, 내용)
     * @param email   요청한 유저 이메일
     * @return 생성된 댓글의 ID
     */
    @Transactional
    public Long createComment(CommentRequest request, String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Comment.CommentBuilder builder = Comment.builder()
                .member(member)
                .content(request.getContent())
                .createdAt(LocalDateTime.now());

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다."));
            builder.parent(parent);
        }

        if ("POST".equalsIgnoreCase(request.getTargetType())) {
            Post post = postRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
            builder.post(post);
        } else if ("REVIEW".equalsIgnoreCase(request.getTargetType())) {
            Review review = reviewRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
            builder.review(review);
        } else {
            throw new IllegalArgumentException("유효하지 않은 타겟 타입입니다.");
        }

        return commentRepository.save(builder.build()).getId();
    }

    /**
     * 특정 대상(게시글/리뷰)의 댓글 목록 조회
     *
     * @param targetId   조회 대상 ID
     * @param targetType 대상 타입 (POST 또는 REVIEW)
     * @return 작성일 오름차순으로 정렬된 댓글 목록
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long targetId, String targetType) {
        List<Comment> comments;
        if ("POST".equalsIgnoreCase(targetType)) {
            comments = commentRepository.findByPostIdOrderByCreatedAtAsc(targetId);
        } else if ("REVIEW".equalsIgnoreCase(targetType)) {
            comments = commentRepository.findByReviewIdOrderByCreatedAtAsc(targetId);
        } else {
            throw new IllegalArgumentException("유효하지 않은 타겟 타입입니다.");
        }

        return comments.stream().map(c -> new CommentResponse(
                c.getId(), c.getContent(), c.getMember().getNickname(), c.getCreatedAt(),
                c.getParent() != null ? c.getParent().getId() : null
        )).collect(Collectors.toList());
    }

    /**
     * 회원이 작성한 게시글 목록을 조회합니다.
     *
     * @param email 유저 이메일
     * @param type  조회할 게시글 타입 필터 (예: MEDIA 등)
     * @return 내가 쓴 게시글 목록 (PostResponse 리스트)
     */
    @Transactional(readOnly = true)
    public List<PostResponse> getMyPosts(String username, String type) {
        List<Post> posts;
        if ("MEDIA".equalsIgnoreCase(type)) {
            posts = postRepository.findByMemberUsernameAndBoardTypeOrderByCreatedAtDesc(username, BoardType.MEDIA);
        } else {
            // MEDIA를 제외한 일반 텍스트 게시글들 조회
            posts = postRepository.findByMemberUsernameAndBoardTypeNotOrderByCreatedAtDesc(username, BoardType.MEDIA);
        }
        return posts.stream().map(p -> new PostResponse(
                p.getId(), p.getTitle(), p.getContent(), p.getBoardType().name(), p.getImageUrl(),
                p.getMember().getNickname(), p.getViewCount(), p.getLikeCount(), p.getCreatedAt()
        )).collect(Collectors.toList());
    }

    /**
     * 회원이 작성한 댓글 목록을 조회합니다. (원글 제목 등 상세 정보 매핑 포함)
     *
     * @param email 유저 이메일
     * @return 내가 쓴 댓글 목록 (MyCommentResponse 리스트)
     */
    @Transactional(readOnly = true)
    public List<MyCommentResponse> getMyComments(String username) {
        List<Comment> comments = commentRepository.findByMemberUsernameOrderByCreatedAtDesc(username);
        return comments.stream().map(c -> {
            Long targetId = null;
            String targetType = null;
            String targetTitle = "삭제된 원본 대상";

            if (c.getPost() != null) {
                targetId = c.getPost().getId();
                targetType = "POST";
                targetTitle = c.getPost().getTitle();
            } else if (c.getReview() != null) {
                targetId = c.getReview().getId();
                targetType = "REVIEW";
                if (c.getReview().getProduct() != null) {
                    targetTitle = c.getReview().getProduct().getName() + " 리뷰";
                } else {
                    targetTitle = "상품 리뷰";
                }
            }

            return new MyCommentResponse(
                    c.getId(),
                    c.getContent(),
                    c.getMember().getNickname(),
                    c.getCreatedAt(),
                    targetId,
                    targetType,
                    targetTitle
            );
        }).collect(Collectors.toList());
    }

    /**
     * 특정 댓글의 내용을 수정합니다.
     * - 자신이 작성한 댓글만 수정 가능합니다.
     *
     * @param commentId 수정할 댓글 ID
     * @param request   수정할 새 댓글 데이터 DTO
     * @param email     수정을 요청한 유저 이메일
     * @throws IllegalArgumentException 대상 댓글이 존재하지 않거나 권한이 없는 경우
     */
    @Transactional
    public void updateComment(Long commentId, CommentRequest request, String username) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!comment.getMember().getUsername().equalsIgnoreCase(username)) {
            throw new IllegalArgumentException("댓글 수정 권한이 없습니다.");
        }
        comment.updateContent(request.getContent());
    }

    /**
     * 특정 댓글을 삭제합니다.
     * - 자신이 작성한 댓글만 삭제 가능합니다.
     *
     * @param commentId 삭제할 댓글 ID
     * @param email     삭제를 요청한 유저 이메일
     * @throws IllegalArgumentException 대상 댓글이 존재하지 않거나 권한이 없는 경우
     */
    @Transactional
    public void deleteComment(Long commentId, String username) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!comment.getMember().getUsername().equalsIgnoreCase(username)) {
            throw new IllegalArgumentException("댓글 삭제 권한이 없습니다.");
        }
        commentRepository.delete(comment);
    }
}
