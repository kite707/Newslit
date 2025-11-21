package com.newslit.backend.rss.exception;

import com.newslit.backend.global.common.dto.BusinessException;
import com.newslit.backend.global.common.enums.ErrorCode;

public class NoRssFoundException extends BusinessException {
    public NoRssFoundException() {
        super(ErrorCode.RSS_NOT_FOUND);
    }

}
