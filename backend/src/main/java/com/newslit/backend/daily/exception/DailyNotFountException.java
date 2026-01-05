package com.newslit.backend.daily.exception;

import com.newslit.backend.global.common.dto.BusinessException;
import com.newslit.backend.global.common.enums.ErrorCode;

public class DailyNotFountException extends BusinessException {
    public DailyNotFountException() {
        super(ErrorCode.DAILY_NOT_FOUND);
    }
}
