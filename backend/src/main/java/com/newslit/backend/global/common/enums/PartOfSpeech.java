package com.newslit.backend.global.common.enums;

import lombok.Getter;

@Getter
public enum PartOfSpeech {
    NOUN("명사"),
    VERB("동사"),
    ADJECTIVE("형용사"),
    ADVERB("부사"),
    PREPOSITION("전치사"),
    CONJUNCTION("접속사"),
    PRONOUN("대명사"),
    INTERJECTION("감탄사");

    private final String korean;

    PartOfSpeech(String korean) {
        this.korean = korean;
    }
}
