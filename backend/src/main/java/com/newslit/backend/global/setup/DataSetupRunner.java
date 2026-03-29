//package com.newslit.backend.global.setup;
//
//import com.newslit.backend.article.Article;
//import com.newslit.backend.article.ArticleRepository;
//import com.newslit.backend.audio.AudioService;
//import com.newslit.backend.crawl.VoaArticleCrawlerService;
//import com.newslit.backend.crawl.VoaRssCrawlerService;
//import com.newslit.backend.daily.DailyService;
//import com.newslit.backend.daily.exception.DuplicateDailyException;
//import com.newslit.backend.sentence.SentenceService;
//import com.newslit.backend.vocabulary.VocabularyService;
//import java.io.IOException;
//import java.util.List;
//import lombok.RequiredArgsConstructor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//@RequiredArgsConstructor
//@Component
//public class DataSetupRunner implements CommandLineRunner {
//    private static final Logger log = LoggerFactory.getLogger(DataSetupRunner.class);
//
//
//    private final VoaRssCrawlerService voaRssCrawlerService;
//    private final VoaArticleCrawlerService voaArticleCrawlerService;
//    private final DailyService dailyService;
//    private final ArticleRepository articleRepository;
//    private final SentenceService sentenceService;
//    private final VocabularyService vocabularyService;
//    private final AudioService audioService;
//
//    @Override
//    public void run(String... args) throws Exception {
//        voaRssCrawlerService.crawlVoaRssLinks();
//        log.debug("RSS 크롤링 완료");
//        voaArticleCrawlerService.crawlAndSaveArticles(1L);
//
//        List<Long> articleIds = articleRepository.findAll().stream().map(Article::getId).toList();
//
//        articleIds.stream().limit(2).forEach(i -> {
//            try {
//                sentenceService.translateOneParagraph(i);
//                articleRepository.findById(i)
//                        .filter(article -> article.getAudioLink() == null || article.getAudioLink().isEmpty())
//                        .ifPresent(article -> {
//                            try {
//                                log.info("article {} 타임스탬프 시작", i);
//                                audioService.saveTimeStamp(i);
//                            } catch (IOException e) {
//                                throw new RuntimeException(e);
//                            }
//                        });
//                log.info("Article {} 번역 완료", i);
//            } catch (Exception e) {
//                log.error("Article {} 처리 중 오류 발생", i, e);
//                throw new RuntimeException(e);
//            }
//        });
//
//        vocabularyService.translateVocabulary();
//        log.info("Vocabulary 번역 완료");
//
//        articleIds.forEach(id -> {
//            try {
//                dailyService.createChunks(id);
//                log.info("Chunks created successfully for article: {}", id);
//            } catch (DuplicateDailyException e) {
//                log.warn("중복된 Daily가 이미 존재하여 스킵: article {}", id);
//                // 애플리케이션은 계속 진행
//            } catch (Exception e) {
//                log.error("Chunk 생성 중 오류 발생: article {}", id, e);
//                // 다른 예외는 로깅하고 계속 진행하거나, 필요시 재던지기
//            }
//        });
//    }
//}
