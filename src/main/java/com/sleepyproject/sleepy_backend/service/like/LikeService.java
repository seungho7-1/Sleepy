package com.sleepyproject.sleepy_backend.service.like;

import com.sleepyproject.sleepy_backend.domain.board.Post;
import com.sleepyproject.sleepy_backend.domain.like.Likes;
import com.sleepyproject.sleepy_backend.domain.like.TargetType;
import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.review.Review;
import com.sleepyproject.sleepy_backend.repository.board.PostRepository;
import com.sleepyproject.sleepy_backend.repository.like.LikeRepository;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import com.sleepyproject.sleepy_backend.repository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository;

    /**
     * 좋아요 토글(추가/취소) 로직
     *
     * @param targetId      좋아요 대상의 ID (게시글 ID 또는 리뷰 ID)
     * @param targetTypeStr 대상 타입 문자열 (POST 또는 REVIEW)
     * @param email         요청한 유저 이메일
     * @return 좋아요 추가 시 true, 취소 시 false 반환
     */
    @Transactional
    public boolean toggleLike(Long targetId, String targetTypeStr, String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        TargetType targetType = TargetType.valueOf(targetTypeStr.toUpperCase());
        
        Optional<Likes> optionalLike = likeRepository.findByMemberAndTargetIdAndTargetType(member, targetId, targetType);

        if (optionalLike.isPresent()) {
            likeRepository.delete(optionalLike.get());
            if (targetType == TargetType.POST) {
                Post post = postRepository.findById(targetId).orElseThrow();
                post.decrementLikeCount();
            } else if (targetType == TargetType.REVIEW) {
                Review review = reviewRepository.findById(targetId).orElseThrow();
                review.decrementLikeCount();
            }
            return false;
        } else {
            likeRepository.save(Likes.builder()
                    .member(member)
                    .targetId(targetId)
                    .targetType(targetType)
                    .build());
            if (targetType == TargetType.POST) {
                Post post = postRepository.findById(targetId).orElseThrow();
                post.incrementLikeCount();
            } else if (targetType == TargetType.REVIEW) {
                Review review = reviewRepository.findById(targetId).orElseThrow();
                review.incrementLikeCount();
            }
            return true;
        }
    }
}
