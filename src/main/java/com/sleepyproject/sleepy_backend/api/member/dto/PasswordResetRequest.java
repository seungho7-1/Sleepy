package com.sleepyproject.sleepy_backend.api.member.dto;

import lombok.Data;

@Data
public class PasswordResetRequest {
    private String email;
    private String code;
    private String newPassword;
}
