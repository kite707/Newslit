package com.newslit.backend.sentence;

import com.deepl.api.DeepLClient;
import com.deepl.api.DeepLException;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TranslationRetryService {

    static final int MAX_ATTEMPTS = 3;

    private final DeepLClient deepLClient;

    @Retryable(
            maxAttempts = MAX_ATTEMPTS,
            backoff = @Backoff(delay = 500, multiplier = 2, maxDelay = 2000, random = true))
    public String translate(String englishText)
            throws DeepLException, InterruptedException {
        return deepLClient.translateText(englishText, null, "ko").getText();
    }
}
