package com.sleepyproject.sleepy_backend.repository.board;

import com.sleepyproject.sleepy_backend.domain.board.BoardType;
import com.sleepyproject.sleepy_backend.domain.board.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByBoardType(BoardType boardType, Pageable pageable);
    List<Post> findByMemberEmailOrderByCreatedAtDesc(String email);
    List<Post> findByMemberEmailAndBoardTypeOrderByCreatedAtDesc(String email, BoardType boardType);
    List<Post> findByMemberEmailAndBoardTypeNotOrderByCreatedAtDesc(String email, BoardType boardType);
}
