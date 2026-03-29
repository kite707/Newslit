package com.newslit.backend.crawl;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.net.URL;
import org.springframework.stereotype.Component;

@Component
public class RssFeedReader {
    public SyndFeed read(String url) throws Exception {
        return new SyndFeedInput().build(new XmlReader(new URL(url)));
    }
}
