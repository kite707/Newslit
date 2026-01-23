package com.newslit.backend.global.exception;

import com.newslit.backend.global.common.dto.BusinessException;
import com.newslit.backend.global.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException e,
            HttpServletRequest request) {

        log.warn("비즈니스 예외 - Code: {}, Message: {}, Path: {}, Method: {}, IP: {}",
                e.getErrorCode().getCode(),
                e.getMessage(),
                request.getRequestURI(),
                request.getMethod(),
                request.getRemoteAddr());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(e.getErrorCode().getStatus().value())
                .error(e.getErrorCode().getStatus().name())
                .errorCode(e.getErrorCode().getCode())
                .message(e.getMessage())
                .build();

        return ResponseEntity.status(e.getErrorCode().getStatus()).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception e,
            HttpServletRequest request) {

        log.error("예상치 못한 에러 - Path: {}, Method: {}, IP: {}, Type: {}",
                request.getRequestURI(),
                request.getMethod(),
                request.getRemoteAddr(),
                e.getClass().getSimpleName(),
                e);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .errorCode("INTERNAL_ERROR")
                .message("서버 오류가 발생했습니다")
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}