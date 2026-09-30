package com.newslit.backend.audio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TtsClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockWebServer server;
    private TtsClient ttsClient;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        ttsClient = new TtsClient(new OkHttpClient(), objectMapper, "test-key",
                server.url("/v1/audio/speech").toString(), "test-model", "test-voice");
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void 텍스트를_보내면_응답_오디오_바이트를_반환한다() throws Exception {
        byte[] audio = {1, 2, 3, 4};
        server.enqueue(new MockResponse().setBody(new Buffer().write(audio)));

        byte[] result = ttsClient.synthesize("Hello world.");

        assertThat(result).isEqualTo(audio);
    }

    @Test
    void 인증_헤더와_모델_목소리_입력을_요청에_담는다() throws Exception {
        server.enqueue(new MockResponse().setBody("x"));

        ttsClient.synthesize("Hello world.");

        RecordedRequest request = server.takeRequest();
        JsonNode body = objectMapper.readTree(request.getBody().readUtf8());
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-key");
        assertThat(body.get("model").asText()).isEqualTo("test-model");
        assertThat(body.get("voice").asText()).isEqualTo("test-voice");
        assertThat(body.get("input").asText()).isEqualTo("Hello world.");
        assertThat(body.get("response_format").asText()).isEqualTo("mp3");
    }

    @Test
    void API가_실패하면_IOException을_던진다() {
        server.enqueue(new MockResponse().setResponseCode(429));

        assertThatThrownBy(() -> ttsClient.synthesize("Hello world."))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("429");
    }
}
