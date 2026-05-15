package com.newslit.backend.user.exception;

import com.newslit.backend.global.common.dto.BusinessException;
import com.newslit.backend.global.common.enums.ErrorCode;

public class CodeExpiredException extends BusinessException {
    public CodeExpiredException() {
        super(ErrorCode.CODE_EXPIRED);
    }
}
