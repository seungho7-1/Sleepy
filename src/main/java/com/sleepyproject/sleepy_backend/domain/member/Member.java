package com.sleepyproject.sleepy_backend.domain.member;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 회원(사용자) 정보를 정의하는 도메인 엔티티 클래스입니다.
 * - 로그인 정보(이메일, 비밀번호), 프로필 정보(닉네임, 역할), 가입일 기록
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private LocalDateTime createdAt;

    /**
     * 닉네임 수정 메서드 (더티 체킹 방식으로 트랜잭션 내에서 자동 반영됨)
     *
     * @param nickname 변경할 새 닉네임
     */
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateRole(Role role) {
        this.role = role;
    }
}