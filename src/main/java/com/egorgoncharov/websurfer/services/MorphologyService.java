package com.egorgoncharov.websurfer.services;

import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.List;

public interface MorphologyService {
    HashMap<String, Integer> getLemmasStatistics(String text);

    List<String> getLemmas(String text);

    String getSnippets(String snippet, String source);

    String getSnippets(List<String> snippets, String source);

    String getTitle(String html);

    String getPageText(Document doc);
}
