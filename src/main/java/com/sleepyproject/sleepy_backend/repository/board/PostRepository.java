package com.sleepyproject.sleepy_backend.repository.board;

import com.sleepyproject.sleepy_backend.domain.board.BoardType;
import com.sleepyproject.sleepy_backend.domain.board.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByBoardType(BoardType boardType, Pageable pageable);

    @EntityGraph(attributePaths = {"member"})
    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN Comment c ON c.post = p " +
            "WHERE p.boardType = :boardType AND " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Post> findByBoardTypeAndKeyword(@Param("boardType") BoardType boardType, @Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {"member"})
    List<Post> findByMemberUsernameOrderByCreatedAtDesc(String username);

    @EntityGraph(attributePaths = {"member"})
    List<Post> findByMemberUsernameAndBoardTypeOrderByCreatedAtDesc(String username, BoardType boardType);

    @EntityGraph(attributePaths = {"member"})
    List<Post> findByMemberUsernameAndBoardTypeNotOrderByCreatedAtDesc(String username, BoardType boardType);

    long countByCreatedAtAfter(java.time.LocalDateTime date);


    /**
     * 기존 조회수에 추가 조회수(addedCount)를 한 번에 더하는 벌크 업데이트 쿼리
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + :addedCount WHERE p.id = :postId")
    void addViewCount(@Param("postId") Long postId, @Param("addedCount") int addedCount);

    /**
     * 게시글의 좋아요 수를 정확한 최신 값으로 덮어씌우는 벌크 업데이트 쿼리
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.likeCount = :likeCount WHERE p.id = :postId")
    void updateLikeCount(@Param("postId") Long postId, @Param("likeCount") int likeCount);
}
