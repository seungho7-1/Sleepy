package com.sleepyproject.sleepy_backend.api.member.dto;

import lombok.Data;

@Data
public class PasswordResetVerifyCodeRequest {
    private String email;
    private String code;
}
