package com.newslit.backend.audio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticleRepository;
import com.newslit.backend.article.exception.ArticleNotFoundException;
import com.newslit.backend.audio.dto.SegmentDto;
import com.newslit.backend.audio.dto.WhisperResponseDto;
import com.newslit.backend.sentence.Sentence;
import jakarta.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AudioService {

    private static final String WHISPER_MODEL = "whisper-1";
    private static final String RESPONSE_FORMAT = "verbose_json";
    private static final String TIMESTAMP_GRANULARITY = "segment";
    private static final MediaType AUDIO_MEDIA_TYPE = MediaType.parse("audio/mpeg");
    private static final int LONG_SENTENCE_MATCH_LENGTH = 10;
    private static final double SHORT_SENTENCE_MATCH_RATIO = 0.8;

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
    //    private final ObjectStorage objectStorage;
    private final ArticleRepository articleRepository;

    @Transactional
    public void saveTimeStamp(Long articleId) throws IOException {

        Article article = articleRepository.findById(articleId)
                .orElseThrow(ArticleNotFoundException::new);

        File audioFile = downloadAudioFile(article.getAudioDownloadLink());

        //TODO : 추후 주석해제
//        uploadMp3ToStorage(articleId, audioFile);

        try {
            List<SegmentDto> segments = transcribe(audioFile);
            List<Sentence> sentences = article.getSentences().stream()
                    .sorted(Comparator.comparing(Sentence::getOrderIndex))
                    .collect(Collectors.toList());

            matchAndSaveTimestamps(segments, sentences);
        } finally {
            audioFile.delete();
        }

    }

    private File downloadAudioFile(String url) throws IOException {
        Request request = new Request.Builder()
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
                 var outputStream = new java.io.FileOutputStream(tempFile)) {
                inputStream.transferTo(outputStream);
            }
            return tempFile;
        }
    }

    private void matchAndSaveTimestamps(List<SegmentDto> segments, List<Sentence> sentences) {
        int segmentStart = 0;
        for (Sentence sentence : sentences) {
            String englishText = normalizeText(sentence.getEnglishText());
            System.out.println("CurSentence is " + englishText);

            boolean matched = false;

            for (int segmentIdx = segmentStart; segmentIdx < segments.size(); segmentIdx++) {
                String segmentText = normalizeText(segments.get(segmentIdx).getText());
                System.out.println("SegmentText is " + segmentText);

                if (isPartialMatch(englishText, segmentText)) {
                    sentence.setStartTime(segments.get(segmentIdx).getStart());
                    sentence.setEndTime(segments.get(segmentIdx).getEnd());
                    matched = true;
                    System.out.println("[SUCCESS] Matched!");
                    segmentStart = segmentIdx + 1;
                    break;
                }
            }

            if (!matched) {
                System.out.println("[FAILED] " + englishText);
            }
        }
    }

    private String normalizeText(String text) {
        return text.trim()
                .toLowerCase()
                .replaceAll("[,.!?;:\"']", "")
                .replaceAll("\\s+", " ");
    }

    private boolean isPartialMatch(String sentenceText, String segmentText) {
        int minMatchLength = LONG_SENTENCE_MATCH_LENGTH;

        // 둘 중 짧은 문장 기준
        int minLength = Math.min(sentenceText.length(), segmentText.length());

        if (minLength < minMatchLength) {
            // 짧은 문장: 80% 이상 일치
            int matchLength = (int) (minLength * SHORT_SENTENCE_MATCH_RATIO);
            return sentenceText.startsWith(segmentText.substring(0, Math.min(matchLength, segmentText.length())))
                    || segmentText.startsWith(sentenceText.substring(0, Math.min(matchLength, sentenceText.length())));
        } else {
            // 긴 문장: 앞부분 10자 이상 일치
            return sentenceText.startsWith(segmentText.substring(0, minMatchLength))
                    || segmentText.startsWith(sentenceText.substring(0, minMatchLength));
        }
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
        System.out.println("=============mergedSegments=====================");
        mergedSegments.forEach(System.out::println);
        System.out.println("=============mergedSegments=====================");
        return mergedSegments;
    }

//    public void uploadMp3ToStorage(Long articleId, File audioFile) throws IOException {
//        Article article = articleRepository.findById(articleId)
//                .orElseThrow(ArticleNotFoundException::new);
//
//        if (!audioFile.exists()) {
//            throw new IOException("Audio file does not exist: " + audioFile.getPath());
//        }
//
//        String objectName = article.getTitle()
//                .replaceAll("[^a-zA-Z0-9가-힣.-]", "_") + ".mp3";
//
//        // URL 인코딩 처리
//        String encodedObjectName = URLEncoder.encode(objectName, StandardCharsets.UTF_8)
//                .replace("+", "%20");
//
//        try (InputStream inputStream = new FileInputStream(audioFile)) {
//            PutObjectRequest putRequest = PutObjectRequest.builder()
//                    .namespaceName(namespace)
//                    .bucketName(bucket)
//                    .objectName(objectName)
//                    .contentLength(audioFile.length())
//                    .contentType("audio/mpeg")
//                    .putObjectBody(inputStream)
//                    .build();
//
//            objectStorage.putObject(putRequest);
//
//            String audioUrl = String.format(
//                    "https://objectstorage.%s.oraclecloud.com/n/%s/b/%s/o/%s",
//                    region,
//                    namespace,
//                    bucket,
//                    encodedObjectName
//            );
//
//            article.setAudioLink(audioUrl);
//            articleRepository.save(article);
//        }
//    }
}