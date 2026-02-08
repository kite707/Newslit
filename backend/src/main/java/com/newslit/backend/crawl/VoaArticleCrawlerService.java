package com.newslit.backend.crawl;

import com.newslit.backend.article.Article;
import com.newslit.backend.article.ArticleService;
import com.newslit.backend.article.dto.ArticleRequestDto;
import com.newslit.backend.global.common.enums.PartOfSpeech;
import com.newslit.backend.rss.RssService;
import com.newslit.backend.rss.dto.RssResponseDto;
import com.newslit.backend.vocabulary.VocabularyService;
import com.newslit.backend.vocabulary.dto.VocabularyRequestDto;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VoaArticleCrawlerService {
    private final ArticleService articleService;
    private final VocabularyService vocabularyService;
    private final RssService rssService;
    private static final String RSS_URL = "https://learningenglish.voanews.com";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";


    public void crawlAndSaveArticles(Long rssId) {
        try {
            RssResponseDto rssDto = rssService.getRssById(rssId);
            String url = RSS_URL + rssDto.getUrl();

            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(new URL(url)));

            for (SyndEntry entry : feed.getEntries()) {
                String title = entry.getTitle();
                String link = entry.getLink();

                try {
                    CrawledArticle crawledArticle = crawlArticle(link, title);

                    if (crawledArticle == null) {
                        continue;
                    }

                    ArticleRequestDto articleDto = ArticleRequestDto.builder()
                            .title(crawledArticle.getTitle())
                            .originalText(crawledArticle.getContent())
                            .mp3Link(crawledArticle.getMp3Link())
                            .sourceUrl(link)
                            .publishedDate(crawledArticle.getPublishedDate())
                            .source("VOA Learning English")
                            .build();

                    Article article = articleService.saveArticle(articleDto);

                    if (!crawledArticle.getWords().isEmpty()) {
                        saveVocabularies(article.getId(), crawledArticle.getWords());
                    }

                    Thread.sleep(1000);

                } catch (Exception e) {
                }
            }

        } catch (Exception e) {
        }
    }

    private CrawledArticle crawlArticle(String url, String defaultTitle) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .get();

            String title = defaultTitle;
            Element titleElement = doc.selectFirst("h1.title.pg-title");
            if (titleElement != null) {
                title = titleElement.text();
            }
            String mp3Link = extractMp3Link(doc);

            LocalDate publishedDate = extractPublishedDate(doc);

            String content = extractContent(doc);
            if (content == null || content.isEmpty()) {
                return null;
            }

            List<String> words = extractWords(doc);

            return CrawledArticle.builder()
                    .title(title)
                    .content(content)
                    .mp3Link(mp3Link)
                    .publishedDate(publishedDate)
                    .words(words)
                    .build();

        } catch (Exception e) {
            return null;
        }
    }

    private String extractMp3Link(Document doc) {
        String html = doc.html();

        // 방법 1: 정규식으로 mp3 찾기
        Pattern fallbackPattern = Pattern.compile("(https://[^\"]*\\.mp3)\\?download=1");
        Matcher fallbackMatcher = fallbackPattern.matcher(html);
        if (fallbackMatcher.find()) {
            return fallbackMatcher.group(1) + "?download=1";
        }

        // 방법 2: 셀렉터로 mp3 찾기
        Element link = doc.selectFirst("a[href*=_hq.mp3?download=1]");
        if (link != null) {
            return link.attr("href");
        }

        return null;
    }

    private LocalDate extractPublishedDate(Document doc) {
        try {
            Element metaDate = doc.selectFirst("meta[name=pubdate]");
            if (metaDate != null && metaDate.hasAttr("content")) {
                String dateStr = metaDate.attr("content");
                return parseDate(dateStr);
            }

            Element timeElement = doc.selectFirst("time");
            if (timeElement != null) {
                String dateStr = timeElement.attr("datetime");
                if (dateStr == null || dateStr.isEmpty()) {
                    dateStr = timeElement.text();
                }
                return parseDate(dateStr);
            }
        } catch (Exception e) {
        }
        return null;
    }

    private LocalDate parseDate(String dateStr) {
        try {
            if (dateStr.contains("T")) {
                ZonedDateTime zdt = ZonedDateTime.parse(dateStr);
                return zdt.toLocalDate();
            }
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractContent(Document doc) {
        Element articleBody = doc.selectFirst("div.wsw");
        if (articleBody == null) {
            return null;
        }

        List<String> paragraphs = new ArrayList<>();
        Elements children = articleBody.children();

        for (Element elem : children) {
            if (elem.tagName().equals("h2") && elem.hasClass("wsw__h2")) {
                break;
            }

            if (elem.tagName().equals("p")) {
                String text = elem.text().trim();
                if (!text.isEmpty()) {
                    paragraphs.add(text);
                }
            }
        }

        return String.join("\n\n", paragraphs);
    }

    private List<String> extractWords(Document doc) {
        List<String> words = new ArrayList<>();
        Element wordsH2 = doc.selectFirst("h2.wsw__h2");

        if (wordsH2 != null) {
            Elements siblings = wordsH2.nextElementSiblings();
            for (Element sibling : siblings) {
                if (sibling.tagName().equals("p")) {
                    String text = sibling.text().trim();
                    if (!text.isEmpty() && !text.equals("Forum")) {
                        words.add(text);
                    }
                }
            }
        }

        return words;
    }

    private void saveVocabularies(Long articleId, List<String> wordTexts) {
        for (String wordText : wordTexts) {
            try {
                VocabularyRequestDto vocabDto = parseVocabulary(articleId, wordText);
                vocabularyService.saveVocabulary(vocabDto);

                Thread.sleep(300);
            } catch (Exception e) {
            }
        }
    }

    private VocabularyRequestDto parseVocabulary(Long articleId, String wordText) {
        String word = wordText;
        PartOfSpeech partOfSpeech = PartOfSpeech.NOUN;

        if (wordText.contains("–")) {
            word = wordText.split("–")[0].trim();
        } else if (wordText.contains("-")) {
            word = wordText.split("-")[0].trim();
        }

        Pattern posPattern = Pattern.compile("\\(([nvadj\\.]+)\\)");
        Matcher matcher = posPattern.matcher(word);

        if (matcher.find()) {
            String posAbbr = matcher.group(1).replace(".", "").trim().toLowerCase();
            partOfSpeech = mapPartOfSpeech(posAbbr);
            word = word.replaceAll("\\s*\\([^)]*\\)", "").trim();
        }

        return VocabularyRequestDto.builder()
                .articleId(articleId)
                .word(word)
                .partOfSpeech(partOfSpeech)
                .build();
    }

    private PartOfSpeech mapPartOfSpeech(String abbr) {
        return switch (abbr) {
            case "n" -> PartOfSpeech.NOUN;
            case "v" -> PartOfSpeech.VERB;
            case "adj" -> PartOfSpeech.ADJECTIVE;
            case "adv" -> PartOfSpeech.ADVERB;
            case "prep" -> PartOfSpeech.PREPOSITION;
            case "conj" -> PartOfSpeech.CONJUNCTION;
            case "pron" -> PartOfSpeech.PRONOUN;
            case "interj" -> PartOfSpeech.INTERJECTION;
            default -> PartOfSpeech.NOUN;
        };
    }

    @Data
    @Builder
    private static class CrawledArticle {
        private String title;
        private String content;
        private String mp3Link;
        private LocalDate publishedDate;
        private List<String> words;
    }
}