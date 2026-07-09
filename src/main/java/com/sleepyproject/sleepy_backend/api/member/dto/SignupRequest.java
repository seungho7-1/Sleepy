package com.sleepyproject.sleepy_backend.api.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
//회원가입 요청
public class SignupRequest {

    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    // 사용자 이메일 (로그인 ID로 사용되며, 중복 불가)
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    // 사용자 비밀번호 (최소 8자 이상, 서버 저장 시 해싱됨)
    private String password;

    @NotBlank(message = "닉네임은 필수 입력값입니다.")
    // 앱 내에서 노출될 사용자 닉네임
    private String nickname;

    // 사용자 역할 (예: BUYER, SELLER). 기본값은 BUYER이나 회원가입 화면에서 선택 가능
    private String role;
}