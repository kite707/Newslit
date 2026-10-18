package com.newslit.backend.article;

public enum ArticlePipelineStage {
    SENTENCE_SPLIT,
    TRANSLATION,
    DAILY_CHUNK,
    COMPLETED
}
