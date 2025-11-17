package com.newslit.backend.user.exception;

import com.newslit.backend.global.common.dto.BusinessException;
import com.newslit.backend.global.common.enums.ErrorCode;

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }
}
