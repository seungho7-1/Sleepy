package com.sleepyproject.sleepy_backend.repository.board;

import com.sleepyproject.sleepy_backend.domain.board.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
    List<Comment> findByReviewIdOrderByCreatedAtAsc(Long reviewId);
}
