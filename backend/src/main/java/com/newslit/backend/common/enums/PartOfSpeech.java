package com.newslit.backend.common.enums;

import lombok.Getter;

@Getter
public enum PartOfSpeech {
    NOUN("명사"),
    VERB("동사"),
    ADJECTIVE("형용사");

    private final String korean;

    PartOfSpeech(String korean) {
        this.korean = korean;
    }
}
