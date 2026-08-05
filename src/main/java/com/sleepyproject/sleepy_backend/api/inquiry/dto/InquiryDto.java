package com.sleepyproject.sleepy_backend.api.inquiry.dto;

import com.sleepyproject.sleepy_backend.domain.inquiry.Inquiry;
import com.sleepyproject.sleepy_backend.domain.inquiry.InquiryStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class InquiryDto {

    @Getter
    @NoArgsConstructor
    public static class Request {
        private String title;
        private String content;
    }

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private String content;
        private String reply;
        private InquiryStatus status;
        private LocalDateTime createdAt;

        public static Response fromEntity(Inquiry inquiry) {
            return Response.builder()
                    .id(inquiry.getId())
                    .title(inquiry.getTitle())
                    .content(inquiry.getContent())
                    .reply(inquiry.getReply())
                    .status(inquiry.getStatus())
                    .createdAt(inquiry.getCreatedAt())
                    .build();
        }
    }
}
