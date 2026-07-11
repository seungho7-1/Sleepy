package com.sleepyproject.sleepy_backend.api.member.dto;

import lombok.Getter;

/**
 * 닉네임 수정 요청 DTO
 * 마이페이지에서 닉네임 변경 시 클라이언트로부터 전달받습니다.
 */
@Getter
@lombok.Setter
@lombok.NoArgsConstructor
public class NicknameUpdateRequest {

    // 변경할 새 닉네임
    private String nickname;

    public NicknameUpdateRequest(String nickname) {
        this.nickname = nickname;
    }
}
