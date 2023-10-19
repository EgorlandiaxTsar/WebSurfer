package com.egorgoncharov.websurfer.dto.indexing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class PageParser {
    private static final Logger LOGGER = LogManager.getLogger(PageParser.class);
    private static final int REQUEST_TIMEOUT = 0;
    private static final String USER_AGENT = "WebSurfer-SearchBot/1.0";
    private static final String REFERRER = "https://google.com";

    public static DocumentResponse getDocument(URL url) {
        Connection connection = Jsoup
                .connect(url.toString())
                .userAgent(USER_AGENT)
                .referrer(REFERRER)
                .timeout(REQUEST_TIMEOUT)
                .ignoreHttpErrors(true);
        try {
            Document doc = connection.get();
            return new DocumentResponse(connection.response().statusCode(), doc);
        } catch (IOException e) {
            LOGGER.warn("Failed to fetch following page: \"" + url + "\"");
        }
        return new DocumentResponse(500, null);
    }

    public static List<SiteTree> getChildPages(URL url, Document doc) {
        if (doc == null) return new ArrayList<>();
        List<Element> links = new ArrayList<>(doc.getElementsByTag("a"));
        Set<String> filteredLinks = new TreeSet<>();
        for (Element link : links) {
            if (!link.attributes().hasKey("href")) {
                continue;
            }
            String href = link.attr("abs:href").replaceFirst("www\\.", "");
            String urlStr = url.toString().replaceFirst("www\\.", "");
            String host = (url.getProtocol() + "://" + url.getHost()).replaceFirst("www\\.", "");
            if (href.contains("#") || href.contains("javascript") || !href.startsWith(urlStr) || href.equals(urlStr) || href.endsWith(".pdf")) {
                continue;
            }
            if (!href.startsWith("/") && !href.startsWith(host)) {
                continue;
            }
            href = href.replaceFirst(host, "");
            if (!href.endsWith("/")) href += "/";
            filteredLinks.add(href);
        }
        List<SiteTree> output = new ArrayList<>();
        filteredLinks.forEach(filteredLink -> output.add(new SiteTree(url, filteredLink)));
        return output;
    }
}

