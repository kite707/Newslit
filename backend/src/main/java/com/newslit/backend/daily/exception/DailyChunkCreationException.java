package com.newslit.backend.daily.exception;

import com.newslit.backend.global.common.dto.BusinessException;
import com.newslit.backend.global.common.enums.ErrorCode;

public class DailyChunkCreationException extends BusinessException {
    public DailyChunkCreationException(String message) {
        super(ErrorCode.DAILY_CHUNK_FAILED, message);
    }
}
