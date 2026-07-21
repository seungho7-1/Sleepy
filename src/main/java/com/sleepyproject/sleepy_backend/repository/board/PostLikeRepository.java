package com.sleepyproject.sleepy_backend.repository.board;

import com.sleepyproject.sleepy_backend.domain.board.Post;
import com.sleepyproject.sleepy_backend.domain.board.PostLike;
import com.sleepyproject.sleepy_backend.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByMemberAndPost(Member member, Post post);
    boolean existsByMemberAndPost(Member member, Post post);
    long countByPost(Post post);
    java.util.List<PostLike> findByMemberAndPostIdIn(Member member, java.util.List<Long> postIds);
    void deleteAllByPost(Post post);
}
