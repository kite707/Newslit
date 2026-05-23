package com.newslit.backend.user.exception;

import com.newslit.backend.global.common.dto.BusinessException;
import com.newslit.backend.global.common.enums.ErrorCode;

public class EmailMismatchException extends BusinessException {
    public EmailMismatchException() {
        super(ErrorCode.EMAIL_MISMATCH);
    }
}
