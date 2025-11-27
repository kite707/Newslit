package com.newslit.backend.vocabulary.exception;

import com.newslit.backend.global.common.dto.BusinessException;
import com.newslit.backend.global.common.enums.ErrorCode;

public class DuplicateVocabularyException extends BusinessException {

    public DuplicateVocabularyException() {
        super(ErrorCode.DUPLICATE_VOCABULARY);
    }
}
