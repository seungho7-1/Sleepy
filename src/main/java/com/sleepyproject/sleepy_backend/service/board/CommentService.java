package com.sleepyproject.sleepy_backend.service.board;

import com.sleepyproject.sleepy_backend.api.board.dto.CommentRequest;
import com.sleepyproject.sleepy_backend.api.board.dto.CommentResponse;
import com.sleepyproject.sleepy_backend.api.board.dto.MyCommentResponse;
import com.sleepyproject.sleepy_backend.domain.board.Comment;
import com.sleepyproject.sleepy_backend.domain.board.Post;
import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.review.Review;
import com.sleepyproject.sleepy_backend.repository.board.CommentRepository;
import com.sleepyproject.sleepy_backend.repository.board.PostRepository;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import com.sleepyproject.sleepy_backend.repository.review.ReviewRepository;
import com.sleepyproject.sleepy_backend.service.member.MemberService;
import com.sleepyproject.sleepy_backend.service.notification.NotificationService;
import com.sleepyproject.sleepy_backend.util.BadWordFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.sleepyproject.sleepy_backend.service.member.MemberReader;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CommentService {
    private final MemberRepository memberRepository;
    private final MemberReader memberReader;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;
    private final BadWordFilter badWordFilter;

    @Transactional
    public Long createComment(CommentRequest request, String username) {
        Member member = memberReader.getMember(username);

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
            builder.post(post);
        } else if ("REVIEW".equalsIgnoreCase(request.getTargetType())) {
            Review review = reviewRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
            builder.review(review);
        } else {
            throw new IllegalArgumentException("유효하지 않은 타겟 타입입니다.");
        }

        Comment savedComment = commentRepository.save(builder.build());

        // 게시글 댓글 수 동기화
        if (savedComment.getPost() != null) {
            savedComment.getPost().incrementCommentCount();
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
        if (!comment.getMember().getUsername().equalsIgnoreCase(username)) {
            throw new IllegalArgumentException("댓글 수정 권한이 없습니다.");
        }
        comment.updateContent(badWordFilter.filter(request.getContent()));
    }

    @Transactional
    public void deleteComment(Long commentId, String username) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!comment.getMember().getUsername().equalsIgnoreCase(username)) {
            throw new IllegalArgumentException("댓글 삭제 권한이 없습니다.");
        }
        // 게시글 댓글 수 동기화
        if (comment.getPost() != null) {
            comment.getPost().decrementCommentCount();
        }
        commentRepository.delete(comment);
    }
}
