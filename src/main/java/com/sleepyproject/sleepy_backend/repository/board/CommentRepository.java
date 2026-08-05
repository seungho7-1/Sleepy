package com.sleepyproject.sleepy_backend.repository.board;

import com.sleepyproject.sleepy_backend.domain.board.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = {"member"})
    List<Comment> findByPostIdAndIsHiddenFalseOrderByCreatedAtAsc(Long postId);
    
    @EntityGraph(attributePaths = {"member"})
    List<Comment> findByReviewIdAndIsHiddenFalseOrderByCreatedAtAsc(Long reviewId);
    
    @EntityGraph(attributePaths = {"member", "post", "review"})
    List<Comment> findByMemberUsernameAndIsHiddenFalseOrderByCreatedAtDesc(String username);

    @EntityGraph(attributePaths = {"member", "post", "review"})
    Page<Comment> findByMemberUsernameAndIsHiddenFalseOrderByCreatedAtDesc(String username, org.springframework.data.domain.Pageable pageable);
    
    int countByPostIdAndIsHiddenFalse(Long postId);
    
    long countByCreatedAtAfter(java.time.LocalDateTime date);
    
    void deleteAllByPost(com.sleepyproject.sleepy_backend.domain.board.Post post);

    @Query("""
    SELECT c.post.id, COUNT(c)
    FROM Comment c
    WHERE c.post.id IN :postIds
      AND c.isHidden = false
    GROUP BY c.post.id
""")
    List<Object[]> countCommentsByPostIds(@Param("postIds") List<Long> postIds);


}
