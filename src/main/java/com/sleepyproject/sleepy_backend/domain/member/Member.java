package com.sleepyproject.sleepy_backend.domain.member;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
}