package com.newslit.backend.article.exception;

import com.newslit.backend.global.common.dto.BusinessException;
import com.newslit.backend.global.common.enums.ErrorCode;

public class DuplicateArticleException extends BusinessException {
    public DuplicateArticleException() {
        super(ErrorCode.DUPLICATE_ARTICLE);
    }
}
