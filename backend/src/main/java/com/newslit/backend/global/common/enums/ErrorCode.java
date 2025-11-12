package com.newslit.backend.global.common.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "CMN-001", "잘못된 입력입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "CMN-002", "서버 오류가 발생했습니다"),

    // Article
    DUPLICATE_ARTICLE(HttpStatus.CONFLICT, "ART-001", "이미 존재하는 게시글입니다"),
    ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "ART-002", "게시글을 찾을 수 없습니다"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USR-001", "사용자를 찾을 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

}
