package com.egorgoncharov.websurfer.services.impl;

import com.egorgoncharov.websurfer.services.MorphologyService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MorphologyServiceImpl implements MorphologyService {
    private static final Logger LOGGER = LogManager.getLogger(MorphologyService.class);
    private static final String[] PARTICLES = {"МЕЖД", "ПРЕДЛ", "СОЮЗ"};
    private static final String RUSSIAN_WORD_REGEX = ".*[А-Яа-яЁё]+.*";
    private final LuceneMorphology morphology;

    public static MorphologyServiceImpl getInstance() {
        try {
            return new MorphologyServiceImpl(new RussianLuceneMorphology());
        } catch (IOException e) {
            LOGGER.error("Failed to create Morphology Service, Lucene Morphology threw IOException");
            return null;
        }
    }

    private MorphologyServiceImpl(RussianLuceneMorphology morphology) {
        this.morphology = morphology;
    }

    @Override
    public HashMap<String, Integer> getLemmasStatistics(String text) {
        HashMap<String, Integer> lemmas = new HashMap<>();
        if (text == null) {
            return lemmas;
        }
        if (text.isEmpty()) {
            return lemmas;
        }
        String[] words = text.split("\\s+");
        for (String word : words) {
            String wordNormalForm = getWord(word);
            if (wordNormalForm == null) {
                continue;
            }
            if (lemmas.containsKey(wordNormalForm)) {
                lemmas.put(wordNormalForm, lemmas.get(wordNormalForm) + 1);
            } else {
                lemmas.put(wordNormalForm, 1);
            }
        }
        return lemmas;
    }

    @Override
    public List<String> getLemmas(String text) {
        List<String> lemmas = new ArrayList<>();
        if (text == null) {
            return lemmas;
        }
        if (text.isEmpty()) {
            return lemmas;
        }
        String[] words = text.split("\\s+");
        for (String word : words) {
            String wordNormalForm = getWord(word);
            if (wordNormalForm == null) {
                continue;
            }
            if (!lemmas.contains(wordNormalForm)) {
                lemmas.add(wordNormalForm);
            }
        }
        return lemmas;
    }

    @Override
    public String getSnippets(String snippet, String source) {
        return getSnippets(new ArrayList<>() {{
            add(snippet);
        }}, source);
    }

    @Override
    public String getSnippets(List<String> snippets, String source) {
        String[] wholeText = getPageText(Jsoup.parse(source)).split(" ");
        int startPoint = 0;
        for (int i = 0; i < wholeText.length; i++) {
            String word = wholeText[i];
            String normalWordForm = getWord(word);
            if (normalWordForm == null) continue;
            if (snippets.contains(normalWordForm)) {
                startPoint = i;
                break;
            }
        }
        int endPoint = startPoint + 6;
        StringBuilder snippet = new StringBuilder();
        for (int i = startPoint; i < (wholeText.length - 1 < endPoint ? wholeText.length : endPoint); i++) {
            String word = wholeText[i];
            String normalWordForm = getWord(word);
            if (normalWordForm == null) continue;
            String snippetPart = "";
            if (snippets.contains(normalWordForm)) {
                snippetPart += "<b>" + word + "</b>";
            } else {
                snippetPart += word;
            }
            snippetPart += " ";
            snippet.append(snippetPart);
        }
        return snippet.toString();
    }

    @Override
    public String getTitle(String html) {
        return Jsoup.parse(html).getElementsByTag("title").get(0).text();
    }

    @Override
    public String getPageText(Document doc) {
        return doc.wholeText().trim().replaceAll("\n", "").replaceAll("\\s+", " ");
    }

    private boolean isWord(String word) {
        return Arrays.stream(PARTICLES).noneMatch(word::contains);
    }

    private boolean isRussianWord(String word) {
        return word.matches(RUSSIAN_WORD_REGEX);
    }

    private String getWord(String word) {
        word = word.toLowerCase().replaceAll("[^А-Яа-яЁё]", "").replaceAll("ё", "е").replaceAll("Ё", "Е");
        if (word.isEmpty()) {
            return null;
        }
        if (!isRussianWord(word)) {
            return null;
        }
        if (!isWord(morphology.getMorphInfo(word).get(0))) {
            return null;
        }
        return getWordNormalForm(word);
    }

    private String getWordNormalForm(String word) {
        List<String> normalForms = morphology.getNormalForms(word);
        return normalForms.isEmpty() ? null : normalForms.get(0);
    }
}
