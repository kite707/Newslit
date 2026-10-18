package com.newslit.backend.global.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class PipelineFailureLogListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void logFinalFailure(PipelineFinalFailureEvent event) {
        log.error(
                "pipelineFinalFailure=true articleId={} stage={} retryCount={} reason={} occurredAt={}",
                event.articleId(),
                event.stage(),
                event.retryCount(),
                event.reason(),
                event.occurredAt()
        );
    }
}
