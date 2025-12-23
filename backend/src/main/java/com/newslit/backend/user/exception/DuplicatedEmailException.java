package com.newslit.backend.user.exception;

import com.newslit.backend.global.common.dto.BusinessException;
import com.newslit.backend.global.common.enums.ErrorCode;

public class DuplicatedEmailException extends BusinessException {
    public DuplicatedEmailException() {
        super(ErrorCode.DUPLICATE_EMAIL);
    }
}
