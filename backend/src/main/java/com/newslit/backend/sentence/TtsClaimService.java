package com.newslit.backend.sentence;

import com.newslit.backend.global.common.enums.Status;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TtsClaimService {
    private static final List<Status> CLAIMABLE_STATUSES = List.of(Status.PENDING, Status.FAILED);

    private final SentenceRepository sentenceRepository;

    @Transactional
    public boolean claim(Long sentenceId, int maxRetryCount) {
        return sentenceRepository.claimTts(
                sentenceId,
                CLAIMABLE_STATUSES,
                Status.PROCESSING,
                maxRetryCount,
                LocalDateTime.now()
        ) == 1;
    }
}
