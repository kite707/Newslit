package com.newslit.backend.audio;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TtsClient {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
    private static final String RESPONSE_FORMAT = "mp3";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String url;
    private final String model;
    private final String voice;

    public TtsClient(OkHttpClient httpClient,
                     ObjectMapper objectMapper,
                     @Value("${openai.api-key}") String apiKey,
                     @Value("${openai.tts.url}") String url,
                     @Value("${openai.tts.model}") String model,
                     @Value("${openai.tts.voice}") String voice) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.url = url;
        this.model = model;
        this.voice = voice;
    }

    public byte[] synthesize(String text) throws IOException {
        String json = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "voice", voice,
                "input", text,
                "response_format", RESPONSE_FORMAT
        ));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(json, JSON_MEDIA_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("TTS API 실패: " + response.code());
            }
            return response.body().bytes();
        }
    }
}
