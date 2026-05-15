package com.newslit.backend.user.exception;

import com.newslit.backend.global.common.dto.BusinessException;
import com.newslit.backend.global.common.enums.ErrorCode;

public class CodeNotFoundException extends BusinessException {
    public CodeNotFoundException() {
        super(ErrorCode.CODE_NOT_FOUND);
    }
}
