package com.newslit.backend.audio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.article.exception.ArticleNotFoundException;
import com.newslit.backend.audio.dto.WhisperResponseDto;
import com.newslit.backend.audio.dto.WordDto;
import com.newslit.backend.sentence.Sentence;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import jakarta.transaction.Transactional;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
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

    @Transactional
    public void saveTimeStamp(Long articleId) throws IOException {

        Article article = articleRepository.findById(articleId)
                .orElseThrow(ArticleNotFoundException::new);

        File audioFile = downloadAudioFile(article.getAudioDownloadLink());

        uploadMp3ToStorage(articleId, audioFile);

        try {
            List<WordDto> words = transcribe(audioFile, article);
            List<Sentence> sentences = article.getSentences().stream()
                    .sorted(Comparator.comparing(Sentence::getOrderIndex))
                    .collect(Collectors.toList());

            matchAndSaveTimestamps(words, sentences);
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

    private void matchAndSaveTimestamps(List<WordDto> words, List<Sentence> sentences) {
        int wordsStart = 0;
        for (Sentence sentence : sentences) {
            int endIdx = getEndIdx(sentence, words, wordsStart);

            sentence.setStartTime(words.get(wordsStart).getStart());
            sentence.setEndTime(words.get(endIdx).getEnd());

            wordsStart = endIdx + 1;
        }
    }

    private int getEndIdx(Sentence sentence, List<WordDto> words, int start) {
        int startIdx = start;
        List<String> sentenceWords = List.of(sentence.getEnglishText().split(" "));
        for (String word : sentenceWords) {
            for (int i = startIdx; i < words.size(); i++) {
                String curWord = words.get(i).getWord();
                if (isSimilar(word, curWord)) {
                    startIdx++;
                    break;
                }
            }
        }
        return startIdx - 1;
    }

    private String normalizeText(String text) {
        return text.replaceAll("[^\\w\\s]", "")  // 특수문자 제거 (공백은 유지)
                .replaceAll("\\s+", "")       // 연속 공백을 하나로
                .toLowerCase()
                .trim();
    }

    private boolean isSimilar(String word1, String word2) {
        word1 = normalizeText(word1);
        word2 = normalizeText(word2);

        if (word1.equals(word2)) {
            return true;
        }

        int distance = levenshteinDistance(word1, word2);
        int maxLen = Math.max(word1.length(), word2.length());
        return distance <= (maxLen * 0.1);

    }

    private int levenshteinDistance(String word1, String word2) {
        int[][] dp = new int[word1.length() + 1][word2.length() + 1];

        for (int i = 1; i <= word1.length(); i++) {
            for (int j = 1; j <= word2.length(); j++) {
                if (word1.charAt(i - 1) == word2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]) + 1;
                }
            }
        }
        return dp[word1.length()][word2.length()];
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


    private void uploadMp3ToStorage(Long articleId, File audioFile) throws IOException {
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

        try (InputStream inputStream = new FileInputStream(audioFile)) {
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

            article.setAudioLink(audioUrl);
            articleRepository.save(article);
        }
    }
}