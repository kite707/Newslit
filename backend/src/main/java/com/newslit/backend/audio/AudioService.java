package com.newslit.backend.audio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.article.exception.ArticleNotFoundException;
import com.newslit.backend.audio.dto.WhisperResponseDto;
import com.newslit.backend.audio.dto.WordDto;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioService {

    private static final String WHISPER_MODEL = "whisper-1";
    private static final String RESPONSE_FORMAT = "verbose_json";
    private static final String TIMESTAMP_GRANULARITY = "word";
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
    private final AudioPersistenceService audioPersistenceService;

    public void saveTimeStamp(Long articleId) throws IOException {

        Article article = articleRepository.findById(articleId)
                .orElseThrow(ArticleNotFoundException::new);

        File audioFile = downloadAudioFile(article.getAudioDownloadLink());

        try {
            String audioUrl = uploadMp3ToStorage(articleId, audioFile);
            List<WordDto> words = transcribe(audioFile, article);

            audioPersistenceService.persistResults(articleId, audioUrl, words);


        } finally {
            audioFile.delete();
        }

    }

    private File downloadAudioFile(String url) throws IOException {
        Request request = new Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to download audio: " + response.code());
            }

            // 임시 파일 생성
            File tempFile = File.createTempFile("audio_", ".mp3");
            try (var inputStream = response.body().byteStream();
                 var outputStream = new FileOutputStream(tempFile)) {
                inputStream.transferTo(outputStream);
            }
            return tempFile;
        }
    }


    private List<WordDto> transcribe(File audioFile, Article article) throws IOException {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", audioFile.getName(),
                        RequestBody.create(audioFile, AUDIO_MEDIA_TYPE))
                .addFormDataPart("model", WHISPER_MODEL)
                .addFormDataPart("response_format", RESPONSE_FORMAT)
                .addFormDataPart("timestamp_granularities[]", TIMESTAMP_GRANULARITY)
                .addFormDataPart("prompt", article.getOriginalText())
                .build();

        Request request = new Builder()
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

            return responseDto.getWords();
        }
    }


    private String uploadMp3ToStorage(Long articleId, File audioFile) throws IOException {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(ArticleNotFoundException::new);

        if (!audioFile.exists()) {
            throw new IOException("Audio file does not exist: " + audioFile.getPath());
        }

        String objectName = article.getTitle()
                .replaceAll("[^a-zA-Z0-9가-힣.-]", "_") + ".mp3";

        // URL 인코딩 처리
        String encodedObjectName = URLEncoder.encode(objectName, StandardCharsets.UTF_8)
                .replace("+", "%20");

        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(audioFile))) {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucket)
                    .objectName(objectName)
                    .contentLength(audioFile.length())
                    .contentType("audio/mpeg")
                    .putObjectBody(inputStream)
                    .build();

            objectStorage.putObject(putRequest);

            String audioUrl = String.format(
                    "https://objectstorage.%s.oraclecloud.com/n/%s/b/%s/o/%s",
                    region,
                    namespace,
                    bucket,
                    encodedObjectName
            );

            return audioUrl;
        }
    }
}