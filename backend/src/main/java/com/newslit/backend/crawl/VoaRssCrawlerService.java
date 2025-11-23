package com.newslit.backend.crawl;

import com.newslit.backend.rss.RssService;
import com.newslit.backend.rss.dto.RssRequestDto;
import java.io.IOException;
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
public class VoaRssCrawlerService {
    private final RssService rssService;
    private static final String RSS_PAGE_URL = "https://learningenglish.voanews.com/rssfeeds";
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    public void crawlVoaRssLinks() throws IOException {

        Document doc = Jsoup.connect(RSS_PAGE_URL)
                .userAgent(USER_AGENT)
                .timeout(10000)
                .get();

        Elements links = doc.select("a.link-service");

        if (links.isEmpty()) {
            return;
        }

        for (Element link : links) {
            String href = link.attr("href");

            String title = findTitle(link);
            String category = findCategory(link);

            RssRequestDto feedInfo = RssRequestDto.builder()
                    .category(category)
                    .title(title)
                    .url(href)
                    .build();

            rssService.saveRss(feedInfo);
            System.out.println(feedInfo);
        }
    }

    private String findTitle(Element link) {
        Element parent = link.parent();

        for (int i = 0; i < 10; i++) {
            if (parent == null) {
                break;
            }

            Element titleElem = parent.selectFirst("h4.media-block__title");
            if (titleElem != null) {
                return titleElem.text().trim();
            }

            parent = parent.parent();
        }

        return "제목 없음";
    }

    private String findCategory(Element link) {
        Element parent = link.parent();

        for (int i = 0; i < 10; i++) {
            if (parent == null) {
                break;
            }

            Element heading = parent.selectFirst("h2, h3");
            if (heading != null) {
                return heading.text().trim();
            }

            parent = parent.parent();
        }

        return "기타";
    }


    @Data
    @Builder
    public static class RssFeedInfo {
        private int index;
        private String category;
        private String title;
        private String url;
    }
}