package com.sleepyproject.sleepy_backend.repository.like;

import com.sleepyproject.sleepy_backend.domain.like.Likes;
import com.sleepyproject.sleepy_backend.domain.like.TargetType;
import com.sleepyproject.sleepy_backend.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LikeRepository extends JpaRepository<Likes, Long> {
    Optional<Likes> findByMemberAndTargetIdAndTargetType(Member member, Long targetId, TargetType targetType);
}
