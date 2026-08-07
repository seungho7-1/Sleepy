package com.sleepyproject.sleepy_backend.service.board;

import com.sleepyproject.sleepy_backend.api.board.dto.CommentRequest;
import com.sleepyproject.sleepy_backend.api.board.dto.CommentResponse;
import com.sleepyproject.sleepy_backend.api.board.dto.MyCommentResponse;
import com.sleepyproject.sleepy_backend.api.board.dto.PostRequest;
import com.sleepyproject.sleepy_backend.api.board.dto.PostResponse;
import com.sleepyproject.sleepy_backend.domain.board.BoardType;
import com.sleepyproject.sleepy_backend.domain.board.Comment;
import com.sleepyproject.sleepy_backend.domain.board.Post;
import com.sleepyproject.sleepy_backend.domain.board.PostLike;
import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.review.Review;
import com.sleepyproject.sleepy_backend.repository.board.CommentRepository;
import com.sleepyproject.sleepy_backend.repository.board.PostLikeRepository;
import com.sleepyproject.sleepy_backend.repository.board.PostRepository;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import com.sleepyproject.sleepy_backend.repository.review.ReviewRepository;
import com.sleepyproject.sleepy_backend.service.notification.NotificationService;
import com.sleepyproject.sleepy_backend.service.upload.VideoCompressionService;
import com.sleepyproject.sleepy_backend.util.BadWordFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;
    private final PostLikeRepository postLikeRepository;
    private final NotificationService notificationService;
    private final PostRedisService postRedisService;
    private final BadWordFilter badWordFilter;
    private final VideoCompressionService videoCompressionService;

    @Transactional
    public Long createPost(PostRequest request, String username) {
        //로그인한 유저가 맞는지 아닌지 확인.
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        //새로운 게시글을 만든 빌더패턴을 이용
        Post post = Post.builder()
                .member(member)
                .title(badWordFilter.filter(request.getTitle()))
                .content(badWordFilter.filter(request.getContent()))
                .boardType(BoardType.valueOf(request.getBoardType().toUpperCase()))
                .imageUrl(request.getImageUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .hashtags(request.getHashtags())
                .createdAt(LocalDateTime.now())
                .isPinned(request.getIsPinned() != null ? request.getIsPinned() : false)
                .build();

        //db에 저장
        Post savedPost = postRepository.save(post);

        //이미지 uirl이 없다면?
        if (request.getImageUrl() != null) {
            videoCompressionService.compressVideoAsync(request.getImageUrl());
        }

        //post_id를 반환
        return savedPost.getId();
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getPosts(String boardTypeStr, String keyword, Pageable pageable, String username) {
        Page<Post> posts;
        if ("ALL".equalsIgnoreCase(boardTypeStr)) {
            posts = postRepository.findByBoardTypeInAndKeyword(
                    asList(BoardType.ALL, BoardType.FREE, BoardType.QNA, BoardType.REVIEW, BoardType.INFO),
                    keyword, pageable);
        } else {
            BoardType type;
            try {
                type = BoardType.valueOf(boardTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("유효하지 않은 게시판 타입입니다. FREE, QNA, NOTICE, MEDIA 중 하나여야 합니다.");
            }
            posts = postRepository.findByBoardTypeAndKeyword(type, keyword, pageable);
        }

        List<Post> content = new java.util.ArrayList<>(posts.getContent());
        List<Post> unpinned = content.stream()
                .filter(p -> !p.isPinned())
                .toList();

        List<Post> sortedContent = new java.util.ArrayList<>();

        if (pageable.getPageNumber() == 0 && keyword.isBlank()
                && !"NOTICE".equalsIgnoreCase(boardTypeStr) && !"MEDIA".equalsIgnoreCase(boardTypeStr)) {
            // 공지사항(NOTICE + isPinned=true) 먼저 상단 고정
            List<Post> pinnedNotices = postRepository.findByIsPinnedTrueAndIsHiddenFalseOrderByCreatedAtDesc().stream()
                    .filter(p -> p.getBoardType() == BoardType.NOTICE)
                    .toList();
            sortedContent.addAll(pinnedNotices);
            // 그 외 isPinned된 일반 글도 추가
            List<Post> globalPinned = postRepository.findByIsPinnedTrueAndIsHiddenFalseOrderByCreatedAtDesc().stream()
                    .filter(p -> p.getBoardType() != BoardType.NOTICE && p.getBoardType() != BoardType.MEDIA)
                    .toList();
            sortedContent.addAll(globalPinned);
        } else if ("NOTICE".equalsIgnoreCase(boardTypeStr) || "MEDIA".equalsIgnoreCase(boardTypeStr)) {
            List<Post> localPinned = content.stream()
                    .filter(Post::isPinned)
                    .sorted(java.util.Comparator.comparing(Post::getCreatedAt).reversed())
                    .toList();
            sortedContent.addAll(localPinned);
        }

        sortedContent.addAll(unpinned);

        posts = new org.springframework.data.domain.PageImpl<>(sortedContent, pageable, posts.getTotalElements());

        List<Long> likedPostIds = new ArrayList<>();
        if (username != null) {
            Member member = memberRepository.findByUsername(username).orElse(null);
            if (member != null) {
                List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
                if (!postIds.isEmpty()) {
                    List<Long> dbLiked = postLikeRepository.findByMemberAndPostIdIn(member, postIds)
                            .stream().map(pl -> pl.getPost().getId()).collect(Collectors.toList());
                    likedPostIds.addAll(dbLiked);
                    for (Long pId : postIds) {
                        if (postRedisService.isLikedByUser(pId, com.sleepyproject.sleepy_backend.domain.like.TargetType.POST, username)) {
                            if (!likedPostIds.contains(pId)) {
                                likedPostIds.add(pId);
                            }
                        }
                    }
                }
            }
        }

        final List<Long> finalLikedPostIds = likedPostIds;
        List<Long> postIds = posts.stream()
                .map(Post::getId)
                .toList();

        Map<Long, Integer> commentCountMap =
                commentRepository.countCommentsByPostIds(postIds)
                        .stream()
                        .collect(Collectors.toMap(
                                row -> (Long) row[0],
                                row -> ((Long) row[1]).intValue()
                        ));
        return posts.map(p -> new PostResponse(
                p.getId(), p.getTitle(), p.getContent(), p.getBoardType().name(), p.getImageUrl(), p.getThumbnailUrl(),
                p.getMember().getNickname(), "ROLE_" + p.getMember().getRole().name(),
                p.getViewCount() + postRedisService.getCachedViewCount(p.getId()),
                p.getLikeCount(),
                p.getCreatedAt(), p.getMember().getProfileImageUrl(), finalLikedPostIds.contains(p.getId()),
                false,
                commentCountMap.getOrDefault(p.getId(), 0),
                p.getPopularityScore(),
                p.isPinned(),
                p.getHashtags()
        ));
    }

    @Transactional
    public PostResponse getPostDetail(Long postId, String username) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        boolean isLiked = false;
        boolean isAdmin = false;
        if (username != null) {
            Member member = memberRepository.findByUsername(username).orElse(null);
            if (member != null) {
                isLiked = postRedisService.isLikedByUser(post.getId(), com.sleepyproject.sleepy_backend.domain.like.TargetType.POST, username)
                        || postLikeRepository.existsByMemberAndPost(member, post);
                isAdmin = member.getRole() == com.sleepyproject.sleepy_backend.domain.member.Role.ADMIN;
            }
        }

        if (post.isHidden()) {
            boolean isAuthor = username != null && post.getMember().getUsername().equals(username);
            if (!isAdmin && !isAuthor) {
                throw new IllegalArgumentException("관리자에 의해 숨김 처리되었거나 접근할 수 없는 게시글입니다.");
            }
        }

        return new PostResponse(
                post.getId(), post.getTitle(), post.getContent(), post.getBoardType().name(), post.getImageUrl(), post.getThumbnailUrl(),
                post.getMember().getNickname(), "ROLE_" + post.getMember().getRole().name(),
                post.getViewCount() + postRedisService.getCachedViewCount(post.getId()), post.getLikeCount(), post.getCreatedAt(), post.getMember().getProfileImageUrl(), isLiked,
                false,
                commentRepository.countByPostIdAndIsHiddenFalse(post.getId()),
                post.getPopularityScore(),
                post.isPinned(),
                post.getHashtags()
        );
    }

    @Transactional
    public PostResponse incrementViewCount(Long postId, String identifier, String username) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        postRedisService.incrementViewCount(postId, identifier);

        boolean isLiked = false;
        if (username != null) {
            Member member = memberRepository.findByUsername(username).orElse(null);
            if (member != null) {
                isLiked = postLikeRepository.existsByMemberAndPost(member, post);
            }
        }

        return new PostResponse(
                post.getId(), post.getTitle(), post.getContent(), post.getBoardType().name(), post.getImageUrl(), post.getThumbnailUrl(),
                post.getMember().getNickname(), "ROLE_" + post.getMember().getRole().name(),
                post.getViewCount() + postRedisService.getCachedViewCount(postId), post.getLikeCount(), post.getCreatedAt(), post.getMember().getProfileImageUrl(), isLiked,
                false,
                commentRepository.countByPostIdAndIsHiddenFalse(post.getId()),
                post.getPopularityScore(),
                post.isPinned(),
                post.getHashtags()
        );
    }

    @Transactional
    public boolean toggleLike(Long postId, String username) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Optional<PostLike> existingLike = postLikeRepository.findByMemberAndPost(member, post);

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            post.decrementLikeCount();
            return false;
        } else {
            postLikeRepository.save(new PostLike(member, post));
            post.incrementLikeCount();
            return true;
        }
    }

    @Transactional
    public void updatePost(Long postId, PostRequest request, String username) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (!post.getMember().getUsername().equals(username) && member.getRole() != com.sleepyproject.sleepy_backend.domain.member.Role.ADMIN) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }
        if (request.getImageUrl() != null && !request.getImageUrl().equals(post.getImageUrl())) {
            videoCompressionService.compressVideoAsync(request.getImageUrl());
        }

        post.update(
                badWordFilter.filter(request.getTitle()),
                badWordFilter.filter(request.getContent()),
                request.getImageUrl(),
                request.getThumbnailUrl(),
                request.getHashtags(),
                request.getIsPinned() != null ? request.getIsPinned() : false
        );
    }

    @Transactional
    public void deletePost(Long postId, String username) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (!post.getMember().getUsername().equals(username) && member.getRole() != com.sleepyproject.sleepy_backend.domain.member.Role.ADMIN) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        commentRepository.deleteAllByPost(post);
        postLikeRepository.deleteAllByPost(post);

        postRepository.delete(post);
    }

    @Transactional
    public Long createComment(CommentRequest request, String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        //댓글 빌더패턴으로 생성
        Comment.CommentBuilder builder = Comment.builder()
                .member(member)
                .content(badWordFilter.filter(request.getContent()))
                .createdAt(LocalDateTime.now());

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다."));
            builder.parent(parent);
        }

        if ("POST".equalsIgnoreCase(request.getTargetType())) {
            Post post = postRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
            // 공지사항 게시글에는 댓글 등록 불가
            if (post.getBoardType() == BoardType.NOTICE) {
                throw new IllegalArgumentException("공지사항에는 댓글을 작성할 수 없습니다.");
            }
            builder.post(post);
        } else if ("REVIEW".equalsIgnoreCase(request.getTargetType())) {
            Review review = reviewRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
            builder.review(review);
        } else {
            throw new IllegalArgumentException("유효하지 않은 타겟 타입입니다.");
        }

        Comment savedComment = commentRepository.save(builder.build());

        // 게시글 댓글 수 원자적 증가 (Race Condition 방지)
        if (savedComment.getPost() != null) {
            postRepository.incrementCommentCount(savedComment.getPost().getId());
        }

        if (savedComment.getParent() != null) {
            Member targetMember = savedComment.getParent().getMember();
            if (!targetMember.getId().equals(member.getId())) {
                String url = savedComment.getPost() != null
                        ? (savedComment.getPost().getBoardType() == com.sleepyproject.sleepy_backend.domain.board.BoardType.MEDIA
                                ? "/shorts?postId=" + savedComment.getPost().getId() + "#comment-" + savedComment.getId()
                                : "/community/" + savedComment.getPost().getId() + "#comment-" + savedComment.getId())
                        : "/product/" + savedComment.getReview().getProduct().getId() + "#comment-" + savedComment.getId();
                notificationService.createNotificationByMember(
                        targetMember,
                        com.sleepyproject.sleepy_backend.domain.notification.NotificationType.NEW_COMMENT,
                        member.getNickname() + "님이 회원님의 댓글에 대댓글을 달았습니다.",
                        url
                );
            }
        } else {
            if (savedComment.getPost() != null) {
                Member targetMember = savedComment.getPost().getMember();
                if (!targetMember.getId().equals(member.getId())) {
                    String url = savedComment.getPost().getBoardType() == com.sleepyproject.sleepy_backend.domain.board.BoardType.MEDIA
                            ? "/shorts?postId=" + savedComment.getPost().getId() + "#comment-" + savedComment.getId()
                            : "/community/" + savedComment.getPost().getId() + "#comment-" + savedComment.getId();
                    notificationService.createNotificationByMember(
                            targetMember,
                            com.sleepyproject.sleepy_backend.domain.notification.NotificationType.NEW_COMMENT,
                            member.getNickname() + "님이 회원님의 게시글에 댓글을 달았습니다.",
                            url
                    );
                }
            } else if (savedComment.getReview() != null) {
                Member targetMember = savedComment.getReview().getMember();
                if (!targetMember.getId().equals(member.getId())) {
                    notificationService.createNotificationByMember(
                            targetMember,
                            com.sleepyproject.sleepy_backend.domain.notification.NotificationType.NEW_COMMENT,
                            member.getNickname() + "님이 회원님의 리뷰에 댓글을 달았습니다.",
                            "/product/" + savedComment.getReview().getProduct().getId() + "#comment-" + savedComment.getId()
                    );
                }
            }
        }

        return savedComment.getId();
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(Long targetId, String targetType) {
        List<Comment> comments;
        if ("POST".equalsIgnoreCase(targetType)) {
            comments = commentRepository.findByPostIdAndIsHiddenFalseOrderByCreatedAtAsc(targetId);
        } else if ("REVIEW".equalsIgnoreCase(targetType)) {
            comments = commentRepository.findByReviewIdAndIsHiddenFalseOrderByCreatedAtAsc(targetId);
        } else {
            throw new IllegalArgumentException("유효하지 않은 타겟 타입입니다.");
        }

        return comments.stream().map(c -> new CommentResponse(
                c.getId(), c.getContent(), c.getMember().getNickname(), c.getCreatedAt(),
                c.getParent() != null ? c.getParent().getId() : null, c.getMember().getProfileImageUrl()
        )).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PostResponse> getMyPosts(String username, String type, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Post> posts;
        if ("MEDIA".equalsIgnoreCase(type)) {
            posts = postRepository.findByMemberUsernameAndBoardTypeAndIsHiddenFalseOrderByCreatedAtDesc(username, BoardType.MEDIA, pageable);
        } else {
            posts = postRepository.findByMemberUsernameAndBoardTypeNotAndIsHiddenFalseOrderByCreatedAtDesc(username, BoardType.MEDIA, pageable);
        }
        return posts.map(p -> new PostResponse(
                p.getId(), p.getTitle(), p.getContent(), p.getBoardType().name(), p.getImageUrl(), p.getThumbnailUrl(),
                p.getMember().getNickname(), "ROLE_" + p.getMember().getRole().name(),
                p.getViewCount(), p.getLikeCount(), p.getCreatedAt(), p.getMember().getProfileImageUrl(), false,
                false,
                commentRepository.countByPostIdAndIsHiddenFalse(p.getId()),
                p.getPopularityScore(),
                p.isPinned(),
                p.getHashtags()
        ));
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<MyCommentResponse> getMyComments(String username, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Comment> comments = commentRepository.findByMemberUsernameAndIsHiddenFalseOrderByCreatedAtDesc(username, pageable);
        return comments.map(c -> {
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
        });
    }

    @Transactional
    public void updateComment(Long commentId, CommentRequest request, String username) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (!comment.getMember().getUsername().equalsIgnoreCase(username)
                && member.getRole() != com.sleepyproject.sleepy_backend.domain.member.Role.ADMIN) {
            throw new IllegalArgumentException("댓글 수정 권한이 없습니다.");
        }
        comment.updateContent(badWordFilter.filter(request.getContent()));
    }

    @Transactional
    public void deleteComment(Long commentId, String username) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (!comment.getMember().getUsername().equalsIgnoreCase(username)
                && member.getRole() != com.sleepyproject.sleepy_backend.domain.member.Role.ADMIN) {
            throw new IllegalArgumentException("댓글 삭제 권한이 없습니다.");
        }
        // 게시글 댓글 수 원자적 감소 (Race Condition 방지)
        if (comment.getPost() != null) {
            postRepository.decrementCommentCount(comment.getPost().getId());
        }
        commentRepository.delete(comment);
    }
}
