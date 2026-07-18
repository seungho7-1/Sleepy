package com.sleepyproject.sleepy_backend.repository.board;

import com.sleepyproject.sleepy_backend.domain.board.BoardType;
import com.sleepyproject.sleepy_backend.domain.board.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByBoardType(BoardType boardType, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN Comment c ON c.post = p " +
           "WHERE p.boardType = :boardType AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Post> findByBoardTypeAndKeyword(@Param("boardType") BoardType boardType, @Param("keyword") String keyword, Pageable pageable);
    List<Post> findByMemberUsernameOrderByCreatedAtDesc(String username);
    List<Post> findByMemberUsernameAndBoardTypeOrderByCreatedAtDesc(String username, BoardType boardType);
    List<Post> findByMemberUsernameAndBoardTypeNotOrderByCreatedAtDesc(String username, BoardType boardType);
    long countByCreatedAtAfter(java.time.LocalDateTime date);
}
