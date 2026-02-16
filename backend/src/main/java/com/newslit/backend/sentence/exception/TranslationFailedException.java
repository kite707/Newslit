package com.newslit.backend.sentence.exception;

import com.newslit.backend.global.common.dto.BusinessException;
import com.newslit.backend.global.common.enums.ErrorCode;

public class TranslationFailedException extends BusinessException {
    public TranslationFailedException() {
        super(ErrorCode.TRANSLATION_FAILED);
    }
}
