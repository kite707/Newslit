package com.newslit.backend.user.exception;

import com.newslit.backend.global.common.dto.BusinessException;
import com.newslit.backend.global.common.enums.ErrorCode;

public class WrongCodeException extends BusinessException {
    public WrongCodeException() {
        super(ErrorCode.WRONG_CODE);
    }
}
