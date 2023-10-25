package com.egorgoncharov.websurfer.services.impl;

import com.egorgoncharov.websurfer.dto.search.SearchResponse;
import com.egorgoncharov.websurfer.dto.search.SearchResponseItem;
import com.egorgoncharov.websurfer.model.entities.IndexEntity;
import com.egorgoncharov.websurfer.model.entities.LemmaEntity;
import com.egorgoncharov.websurfer.model.entities.PageEntity;
import com.egorgoncharov.websurfer.model.repositories.IndexesRepository;
import com.egorgoncharov.websurfer.model.repositories.LemmasRepository;
import com.egorgoncharov.websurfer.services.SearchService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    private static final Logger LOGGER = LogManager.getLogger(SearchService.class);
    private final MorphologyServiceImpl morphologyService = MorphologyServiceImpl.getInstance();
    private final UtilsServiceImpl utils;
    private final LemmasRepository lemmasRepository;
    private final IndexesRepository indexesRepository;

    @Override
    public SearchResponse search(String query, String site, int offset, int limit) {
        if (query == null || query.isEmpty()) {
            LOGGER.warn("Search request rejected: 400, Query is not present");
            return new SearchResponse(false, 400, -1, null, "Поисковый запрос отсутствует");
        }
        if (site == null || site.equals("none")) {
            return globalSiteSearch(query, offset, limit);
        }
        if (!utils.validateUrl(site)) {
            LOGGER.warn("Search request rejected: 400, Site URL is malformed");
            return new SearchResponse(false, 400, -1, null, "Некорректный URL-Адрес");
        }
        return singleSiteSearch(query, site, offset, limit);
    }

    private SearchResponse singleSiteSearch(String query, String site, int offset, int limit) {
        LOGGER.info("Searching through \"" + site + "\", query \"" + query + "\"");
        List<String> lemmas = morphologyService.getLemmas(query);
        if (lemmas.isEmpty()) {
            return new SearchResponse(true, 200, 0, new ArrayList<>(), null);
        }
        List<LemmaEntity> lemmaEntities = findAndSortRelatedLemmas(lemmas, site);
        if (lemmaEntities.isEmpty()) {
            return new SearchResponse(true, 200, 0, new ArrayList<>(), null);
        }
        List<PageEntity> filteredPages = findRelatedPages(lemmaEntities, site);
        if (filteredPages.isEmpty()) {
            return new SearchResponse(true, 200, 0, new ArrayList<>(), null);
        }
        HashMap<PageEntity, Double> relevanceSortedMap = sortPagesByRelevance(filteredPages, lemmas);
        return packSuccessfulResponse(relevanceSortedMap, lemmas, offset, limit);
    }

    private SearchResponse globalSiteSearch(String query, int offset, int limit) {
        LOGGER.info("Searching through all sites, query \"" + query + "\"");
        List<String> lemmas = morphologyService.getLemmas(query);
        if (lemmas.isEmpty()) {
            return new SearchResponse(true, 200, 0, new ArrayList<>(), null);
        }
        List<LemmaEntity> lemmaEntities = findAndSortRelatedLemmas(lemmas);
        if (lemmaEntities.isEmpty()) {
            return new SearchResponse(true, 200, 0, new ArrayList<>(), null);
        }
        List<PageEntity> filteredPages = findRelatedPages(lemmaEntities);
        if (filteredPages.isEmpty()) {
            return new SearchResponse(true, 200, 0, new ArrayList<>(), null);
        }
        HashMap<PageEntity, Double> relevanceSortedMap = sortPagesByRelevance(filteredPages, lemmas);
        return packSuccessfulResponse(relevanceSortedMap, lemmas, offset, limit);
    }

    private List<LemmaEntity> findAndSortRelatedLemmas(List<String> lemmas, String site) {
        List<LemmaEntity> lemmaEntities = new ArrayList<>();
        lemmas.forEach(l -> lemmaEntities.add(lemmasRepository.findLemmasByLemmaAndSiteUrl(l, site)));
        lemmaEntities.removeIf(Objects::isNull);
        return sortRelatedLemmas(lemmaEntities);
    }

    private List<LemmaEntity> findAndSortRelatedLemmas(List<String> lemmas) {
        List<LemmaEntity> lemmaEntities = new ArrayList<>();
        lemmas.forEach(l -> lemmaEntities.addAll(lemmasRepository.findLemmasByLemma(l)));
        lemmaEntities.removeIf(Objects::isNull);
        return sortRelatedLemmas(lemmaEntities);
    }

    private List<LemmaEntity> sortRelatedLemmas(List<LemmaEntity> lemmaEntities) {
        lemmaEntities.sort((o1, o2) -> {
            if (o1.getFrequency() < o2.getFrequency()) {
                return 1;
            } else if (o1.getFrequency() > o2.getFrequency()) {
                return -1;
            } else {
                return o1.getLemma().compareTo(o2.getLemma());
            }
        });
        return lemmaEntities;
    }

    private List<PageEntity> findRelatedPages(List<LemmaEntity> lemmaEntities, String site) {
        LemmaEntity rarestLemma = lemmaEntities.get(0);
        List<PageEntity> pages = new ArrayList<>();
        indexesRepository.findIndexesByLemmaAndSiteUrl(rarestLemma.getLemma(), site).forEach(index -> pages.add(index.getPage()));
        lemmaEntities.remove(0);
        List<PageEntity> filteredPages = new ArrayList<>();
        for (LemmaEntity lemma : lemmaEntities) {
            for (PageEntity page : pages) {
                for (IndexEntity index : page.getIndexes()) {
                    if (index.getLemma().getLemma().equals(lemma.getLemma())) {
                        filteredPages.add(page);
                        break;
                    }
                }
            }
        }
        if (lemmaEntities.isEmpty()) {
            filteredPages.addAll(pages);
        }
        return filteredPages;
    }

    private List<PageEntity> findRelatedPages(List<LemmaEntity> lemmaEntities) {
        LemmaEntity rarestLemma = lemmaEntities.get(0);
        List<PageEntity> pages = new ArrayList<>();
        indexesRepository.findIndexesByLemma(rarestLemma.getLemma()).forEach(index -> pages.add(index.getPage()));
        lemmaEntities.remove(0);
        List<PageEntity> filteredPages = new ArrayList<>();
        for (LemmaEntity lemma : lemmaEntities) {
            for (PageEntity page : pages) {
                for (IndexEntity index : page.getIndexes()) {
                    if (index.getLemma().getLemma().equals(lemma.getLemma())) {
                        filteredPages.add(page);
                        break;
                    }
                }
            }
        }
        if (lemmaEntities.isEmpty()) {
            filteredPages.addAll(pages);
        }
        return filteredPages;
    }

    private HashMap<PageEntity, Double> sortPagesByRelevance(List<PageEntity> pages, List<String> lemmas) {
        Map<PageEntity, Double> calculatedRelevanceMap = new HashMap<>();
        double maxAbsoluteRelevance = 0;
        for (PageEntity page : pages) {
            double absoluteRelevance = 0;
            for (IndexEntity index : page.getIndexes()) {
                if (lemmas.contains(index.getLemma().getLemma())) {
                    absoluteRelevance += index.getRank();
                }
            }
            calculatedRelevanceMap.put(page, absoluteRelevance);
            if (absoluteRelevance > maxAbsoluteRelevance) maxAbsoluteRelevance = absoluteRelevance;
        }
        for (Map.Entry<PageEntity, Double> entry : calculatedRelevanceMap.entrySet()) {
            calculatedRelevanceMap.put(entry.getKey(), entry.getValue() / maxAbsoluteRelevance);
        }
        List<Map.Entry<PageEntity, Double>> listMap = new ArrayList<>(calculatedRelevanceMap.entrySet());
        LinkedHashMap<PageEntity, Double> relevanceSortedMap = new LinkedHashMap<>(calculatedRelevanceMap);
        listMap.sort(((o1, o2) -> {
            if (o1.getValue() > o2.getValue()) {
                return -1;
            } else if (o1.getValue() < o2.getValue()) {
                return 1;
            }
            return 0;
        }));
        relevanceSortedMap.clear();
        listMap.forEach(pageEntityDoubleEntry -> relevanceSortedMap.put(pageEntityDoubleEntry.getKey(), pageEntityDoubleEntry.getValue()));
        return relevanceSortedMap;
    }

    private SearchResponse packSuccessfulResponse(HashMap<PageEntity, Double> relevanceSortedMap, List<String> lemmas, int offset, int limit) {
        LOGGER.info("Search completed (found " + relevanceSortedMap.size() + " results), packing data");
        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setError(null);
        searchResponse.setStatusCode(200);
        searchResponse.setCount(relevanceSortedMap.size());
        searchResponse.setResult(true);
        List<SearchResponseItem> searchResponseItems = new ArrayList<>();
        relevanceSortedMap.forEach((page, rel) -> searchResponseItems.add(new SearchResponseItem(
                page.getSite().getUrl(),
                page.getSite().getName(),
                page.getPath(),
                morphologyService.getTitle(page.getContent()),
                morphologyService.getSnippets(lemmas, page.getContent()),
                rel
        )));
        searchResponse.setData(searchResponseItems.stream().skip(offset).limit(limit).collect(Collectors.toList()));
        return searchResponse;
    }
}
