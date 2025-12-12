package com.newslit.backend.daily.exception;

import com.newslit.backend.global.common.dto.BusinessException;
import com.newslit.backend.global.common.enums.ErrorCode;

public class DuplicateDailyException extends BusinessException {
    public DuplicateDailyException() {
        super(ErrorCode.DUPLICATE_DAILY);
    }
}
