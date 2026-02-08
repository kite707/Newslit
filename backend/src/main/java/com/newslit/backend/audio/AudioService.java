package com.newslit.backend.audio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newslit.backend.audio.dto.SegmentDto;
import com.newslit.backend.audio.dto.WhisperResponseDto;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AudioService {

    private static final String WHISPER_MODEL = "whisper-1";
    private static final String RESPONSE_FORMAT = "verbose_json";
    private static final String TIMESTAMP_GRANULARITY = "segment";
    private static final MediaType AUDIO_MEDIA_TYPE = MediaType.parse("audio/mpeg");

    @Value("${openai.api-key}")
    private String apiKey;
    @Value("${openai.url}")
    private String openAIUrl;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;


    public List<SegmentDto> transcribe(File audioFile) throws IOException {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", audioFile.getName(),
                        RequestBody.create(audioFile, AUDIO_MEDIA_TYPE))
                .addFormDataPart("model", WHISPER_MODEL)
                .addFormDataPart("response_format", RESPONSE_FORMAT)
                .addFormDataPart("timestamp_granularities[]", TIMESTAMP_GRANULARITY)
                .build();

        Request request = new Request.Builder()
                .url(openAIUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(requestBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Whisper API 실패: " + response.code());
            }

            WhisperResponseDto responseDto = objectMapper.readValue(
                    response.body().string(),
                    WhisperResponseDto.class
            );
            return mergeSegments(responseDto);
        }
    }

    private List<SegmentDto> mergeSegments(WhisperResponseDto response) {
        List<SegmentDto> mergedSegments = new ArrayList<>();
        SegmentDto current = null;

        for (SegmentDto segment : response.getSegments()) {
            String text = segment.getText().trim();

            if (current == null) {
                current = new SegmentDto();
                current.setId(mergedSegments.size());
                current.setStart(segment.getStart());
                current.setEnd(segment.getEnd());
                current.setText(text);
            } else {
                current.setText(current.getText() + " " + text);
                current.setEnd(segment.getEnd());
            }

            // 문장 끝 판단 (. ! ? 로 끝나면)
            if (text.endsWith(".") || text.endsWith("!") || text.endsWith("?")) {
                mergedSegments.add(current);
                current = null;
            }
        }

        // 마지막 미완성 문장 처리
        if (current != null) {
            mergedSegments.add(current);
        }
        return mergedSegments;
    }
}