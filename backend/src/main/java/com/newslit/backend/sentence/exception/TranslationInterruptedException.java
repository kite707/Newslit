package com.newslit.backend.sentence.exception;

import com.newslit.backend.global.common.dto.BusinessException;
import com.newslit.backend.global.common.enums.ErrorCode;

public class TranslationInterruptedException extends BusinessException {
    public TranslationInterruptedException() {
        super(ErrorCode.TRANSLATION_INTERRUPTED);
    }
}
