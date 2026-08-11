package com.sleepyproject.sleepy_backend.service.like;

import com.sleepyproject.sleepy_backend.api.like.dto.LikeRequestDto;
import com.sleepyproject.sleepy_backend.api.like.dto.LikeResponseDto;
import com.sleepyproject.sleepy_backend.domain.board.BoardType;
import com.sleepyproject.sleepy_backend.domain.board.Post;
import com.sleepyproject.sleepy_backend.domain.like.Likes;
import com.sleepyproject.sleepy_backend.domain.like.TargetType;
import com.sleepyproject.sleepy_backend.domain.member.Member;
import com.sleepyproject.sleepy_backend.domain.notification.NotificationType;
import com.sleepyproject.sleepy_backend.domain.review.Review;
import com.sleepyproject.sleepy_backend.repository.board.PostRepository;
import com.sleepyproject.sleepy_backend.repository.like.LikeRepository;
import com.sleepyproject.sleepy_backend.repository.member.MemberRepository;
import com.sleepyproject.sleepy_backend.repository.review.ReviewRepository;
import com.sleepyproject.sleepy_backend.service.board.PostRedisService;
import com.sleepyproject.sleepy_backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.sleepyproject.sleepy_backend.service.member.MemberReader;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 게시글 또는 댓글/리뷰 좋아요(추천) 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;
    private final MemberReader memberReader;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;
    private final PostRedisService postRedisService;
    private final LikeAsyncService likeAsyncService;


    /**
     * 좋아요 토글(추가/취소) 로직
     *
     * @param username 요청한 유저 이메일
     * @return 좋아요 추가 시 true, 취소 시 false 반환
     */

    @Transactional
    public LikeResponseDto toggleLike(LikeRequestDto requestDto, String username) {

        //로그인한 유저의 정보조회
        Member member = memberReader.getMember(username);

        //요청온 게시판의 타입 얻기(POST,REVIEW 나중에 COMMENT)
        TargetType targetType = requestDto.getTargetType();

        // Redis에서만 토글 ->
        boolean isLiked = postRedisService.toggleLike(requestDto.getTargetId(), targetType, username);

        // DB는 Async에게 맡김
        likeAsyncService.syncLikeToDatabase(member, requestDto.getTargetId(), targetType, isLiked);

        // 좋아요가 새로 추가됐을 때, 게시글/리뷰 작성자에게 알림을 보내는 메서드
        if (isLiked) {
            //타겟타입에 게시글이면?
            if (targetType == TargetType.POST) {
                //targetId를 찾아보자.
                Post post = postRepository.findById(requestDto.getTargetId()).orElseThrow();
                if (!post.getMember().getId().equals(member.getId())) {
                    String url = post.getBoardType() == BoardType.MEDIA
                        ? "/shorts?postId=" + post.getId() 
                        : "/community/" + post.getId();
                    //해당 게시글 유저에게 알림을 발송
                    notificationService.createNotificationByMember(post.getMember(), NotificationType.NEW_LIKE, member.getNickname() + "님이 회원님의 게시글을 좋아합니다.", url);
                }

            } else {

                Review review = reviewRepository.findById(requestDto.getTargetId()).orElseThrow();
                if (!review.getMember().getId().equals(member.getId())) {
                    //해당 리뷰 유저에게 알림을 발송
                    notificationService.createNotificationByMember(review.getMember(), NotificationType.NEW_LIKE, member.getNickname() + "님이 회원님의 리뷰를 좋아합니다.", "/product/" + review.getProduct().getId());
                }
            }
        }

        int likeCount = postRedisService.getCachedLikeCount(requestDto.getTargetId(), targetType);

        return new LikeResponseDto(isLiked, likeCount);
    }
}
