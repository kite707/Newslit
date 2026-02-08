package com.newslit.backend.audio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.article.exception.ArticleNotFoundException;
import com.newslit.backend.audio.dto.SegmentDto;
import com.newslit.backend.audio.dto.WhisperResponseDto;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.PutObjectResponse;
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
import okhttp3.ResponseBody;
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
    @Value("${oracle.cloud.namespace}")
    private String namespace;

    @Value("${oracle.cloud.bucket}")
    private String bucket;
    @Value("${oracle.cloud.region}")
    private String region;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ObjectStorage objectStorage;
    private final ArticleRepository articleRepository;

    public void saveTimeStamp(Long articleId) throws IOException {
        Article article = articleRepository.findById(articleId).orElseThrow(ArticleNotFoundException::new);
        String url = article.getAudioDownloadLink();

        String objectName = article.getTitle().replaceAll("[^a-zA-Z0-9가-힣.-]", "_") + ".mp3";
        uploadMp3ToStorage(url, objectName);
        String audioUrl = String.format(
                "https://objectstorage.%s.oraclecloud.com/n/%s/b/%s/o/%s",
                region,
                namespace,
                bucket,
                objectName
        );
        article.setAudioLink(audioUrl);
        articleRepository.save(article);
    }

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

    public PutObjectResponse uploadMp3ToStorage(String mp3Url, String objectName) throws IOException {
        Request request = new Request.Builder()
                .url(mp3Url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to download: " + response.code());
            }

            ResponseBody body = response.body();
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucket)
                    .objectName(objectName)
                    .contentLength(body.contentLength())
                    .contentType("audio/mpeg")
                    .putObjectBody(body.byteStream())
                    .build();

            return objectStorage.putObject(putRequest);
        }
    }
}