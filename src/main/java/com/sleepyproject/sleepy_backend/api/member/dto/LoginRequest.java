package com.sleepyproject.sleepy_backend.api.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
//로그인 요청
public class LoginRequest {
    private String email;
    private String password;
}