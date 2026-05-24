package com.newslit.backend.user.exception;

import com.newslit.backend.global.common.dto.BusinessException;
import com.newslit.backend.global.common.enums.ErrorCode;

public class AttemptLimitExceededException extends BusinessException {
    public AttemptLimitExceededException() {
        super(ErrorCode.ATTEMPT_LIMIT_EXCEEDED);
    }
}