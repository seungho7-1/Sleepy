package com.sleepyproject.sleepy_backend.domain.board;

public enum BoardType {
    ALL, FREE, QNA, NOTICE, MEDIA, REVIEW, INFO;


    public static BoardType from(String value) {
        try {
            return BoardType.valueOf(value.toUpperCase());

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "존재하지 않는 게시판입니다."
            );
        }
    }
}
