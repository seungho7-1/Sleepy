package com.sleepyproject.sleepy_backend.repository.board;

import com.sleepyproject.sleepy_backend.domain.board.BoardType;
import com.sleepyproject.sleepy_backend.domain.board.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByBoardType(BoardType boardType, Pageable pageable);
    List<Post> findByMemberUsernameOrderByCreatedAtDesc(String username);
    List<Post> findByMemberUsernameAndBoardTypeOrderByCreatedAtDesc(String username, BoardType boardType);
    List<Post> findByMemberUsernameAndBoardTypeNotOrderByCreatedAtDesc(String username, BoardType boardType);
}
