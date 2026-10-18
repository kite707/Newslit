package com.newslit.backend.article;

public enum ArticlePipelineStage {
    SENTENCE_SPLIT,
    TRANSLATION,
    DAILY_CHUNK,
    COMPLETED,
    /**
     * 문장별 TTS 생성 단계. Article의 전이 상태(pipelineStage)로 저장되지 않고,
     * 최종 실패 알림(PipelineFinalFailureEvent)에서 실패한 단계를 구분하는 데만 쓴다.
     */
    TTS
}
