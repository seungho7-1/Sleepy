package com.sleepyproject.sleepy_backend.api.member.dto;

import lombok.Data;

@Data
public class PasswordResetSendCodeRequest {
    private String username;
    private String email;
}
