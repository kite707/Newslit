package com.newslit.backend.audio;

import static org.assertj.core.api.Assertions.assertThat;

import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.sentence.Sentence;
import com.newslit.backend.sentence.SentenceRepository;
import com.oracle.bmc.objectstorage.ObjectStorage;
import java.io.IOException;
import java.time.LocalDate;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class AudioServiceTest {

    private static final MockWebServer mockWebServer = new MockWebServer();

    static {
        try {
            mockWebServer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @DynamicPropertySource
    static void overrideOpenAiUrl(DynamicPropertyRegistry registry) {
        registry.add("openai.url", () -> mockWebServer.url("/v1/audio/transcriptions").toString());
    }

    @Autowired
    private AudioService audioService;

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private SentenceRepository sentenceRepository;

    // Oracle Cloud Object Storage로 실제 업로드가 나가지 않도록 목으로 대체
    @MockBean
    private ObjectStorage objectStorage;

    private Long articleId;

    @BeforeEach
    void setup() {
        mockWebServer.setDispatcher(new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest request) {
                if (request.getPath() != null && request.getPath().startsWith("/audio")) {
                    return new MockResponse()
                            .setResponseCode(200)
                            .setBody("fake-audio-bytes");
                }
                return new MockResponse()
                        .setResponseCode(200)
                        .addHeader("Content-Type", "application/json")
                        .setBody("""
                                {
                                  "text": "Hello world.",
                                  "language": "english",
                                  "duration": 1.2,
                                  "words": [
                                    {"word": "Hello", "start": 0.0, "end": 0.4},
                                    {"word": "world.", "start": 0.4, "end": 1.0}
                                  ]
                                }
                                """);
            }
        });

        Article article = articleRepository.save(Article.builder()
                .title("Audio Test Article " + System.nanoTime())
                .originalText("Hello world.")
                .publishedDate(LocalDate.now())
                .source("test")
                .audioDownloadLink(mockWebServer.url("/audio.mp3").toString())
                .build());
        articleId = article.getId();

        sentenceRepository.save(Sentence.builder()
                .article(article)
                .orderIndex(0)
                .englishText("Hello world.")
                .build());
    }

    @AfterEach
    void cleanup() {
        sentenceRepository.deleteAll(sentenceRepository.findAllByArticleId(articleId));
        articleRepository.deleteById(articleId);
    }

    @AfterAll
    static void shutdownServer() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void audio_test() throws IOException {
        audioService.saveTimeStamp(articleId);

        Article saved = articleRepository.findById(articleId).orElseThrow();
        assertThat(saved.getAudioLink()).isNotBlank();

        Sentence sentence = sentenceRepository.findAllByArticleId(articleId).get(0);
        assertThat(sentence.getStartTime()).isNotNull();
        assertThat(sentence.getEndTime()).isNotNull();
    }
}
