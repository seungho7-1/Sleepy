package com.sleepyproject.sleepy_backend.repository.board;

import com.sleepyproject.sleepy_backend.domain.board.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = {"member"})
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
    
    @EntityGraph(attributePaths = {"member"})
    List<Comment> findByReviewIdOrderByCreatedAtAsc(Long reviewId);
    
    @EntityGraph(attributePaths = {"member", "post", "review"})
    List<Comment> findByMemberUsernameOrderByCreatedAtDesc(String username);
    long countByCreatedAtAfter(java.time.LocalDateTime date);
    
    void deleteAllByPost(com.sleepyproject.sleepy_backend.domain.board.Post post);
}
