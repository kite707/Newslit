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
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USR-001", "사용자를 찾을 수 없습니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USR-002", "이미 존재하는 이메일입니다."),
    ALREADY_VERIFIED(HttpStatus.CONFLICT, "USR-003", "이미 인증이 완료된 유저입니다."),
    SEND_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "USR-004", "메일 발송 횟수를 초과했습니다."),
    CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "USR-005", "해당 이메일로 등록된 코드를 찾을 수 없습니다."),
    CODE_EXPIRED(HttpStatus.BAD_REQUEST, "USR-006", "만료된 코드입니다."),
    WRONG_CODE(HttpStatus.BAD_REQUEST, "USR-007", "코드가 일치하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "USR-008", "유효하지 않은 토큰입니다."),
    EMAIL_MISMATCH(HttpStatus.BAD_REQUEST, "USR-009", "인증된 이메일과 요청 이메일이 일치하지 않습니다."),

    // RSS
    RSS_NOT_FOUND(HttpStatus.NOT_FOUND, "RSS-002", "RSS 링크를 찾을 수 없습니다"),

    // Vocabulary
    DUPLICATE_VOCABULARY(HttpStatus.CONFLICT, "VOC-001", "이미 존재하는 단어입니다"),

    //Daily
    DUPLICATE_DAILY(HttpStatus.CONFLICT, "DLY-001", "이미 존재하는 일일 컨텐츠입니다"),
    DAILY_NOT_FOUND(HttpStatus.CONFLICT, "DLY-002", "존재하지 않는 일일 컨텐츠입니다"),

    //Sentence
    TRANSLATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "SNT-001", "번역 처리 중 오류가 발생했습니다"),
    TRANSLATION_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "SNT-002", "번역 작업이 중단되었습니다"),
    SENTENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "SNT-003", "존재하지 않는 문장입니다");

    //Email Verify


    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

}
