package com.newslit.backend.audio;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.transaction.Transactional;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
class AudioServiceTest {
    @Autowired
    private AudioService audioService;

    @Test
    void audio_test() {
        try {
            log.info("article {} 타임스탬프 시작", 50);
            audioService.saveTimeStamp(50L);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}