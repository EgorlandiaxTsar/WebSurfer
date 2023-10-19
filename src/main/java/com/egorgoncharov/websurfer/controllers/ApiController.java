package com.egorgoncharov.websurfer.controllers;

import com.egorgoncharov.websurfer.dto.indexing.IndexingResponse;
import com.egorgoncharov.websurfer.dto.search.SearchResponse;
import com.egorgoncharov.websurfer.dto.statistics.StatisticsResponse;
import com.egorgoncharov.websurfer.services.StatisticsService;
import com.egorgoncharov.websurfer.services.impl.IndexingServiceImpl;
import com.egorgoncharov.websurfer.services.impl.SearchServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@SuppressWarnings("DataFlowIssue")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {
    private static final Logger LOGGER = LogManager.getLogger(ApiController.class);
    private final StatisticsService STATISTICS_SERVICE;
    private final IndexingServiceImpl INDEXING_SERVICE;
    private final SearchServiceImpl SEARCH_SERVICE;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        LOGGER.info("Statistics requested (parameters: not-included)");
        return ResponseEntity.ok(STATISTICS_SERVICE.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<String> startIndexing() {
        LOGGER.info("Indexing start requested (parameters: not-included)");
        IndexingResponse indexingResponse = INDEXING_SERVICE.startIndexing();
        return new ResponseEntity<>(indexingResponse.json(), HttpStatus.resolve(indexingResponse.getStatusCode()));
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<String> stopIndexing() {
        LOGGER.info("Indexing stop requested (parameters: not-included)");
        IndexingResponse indexingResponse = INDEXING_SERVICE.stopIndexing();
        return new ResponseEntity<>(indexingResponse.json(), HttpStatus.resolve(indexingResponse.getStatusCode()));
    }

    @GetMapping("/search")
    public ResponseEntity<String> search(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "none") String site,
            @RequestParam(name = "offset", required = false) Optional<Integer> offsetOptional,
            @RequestParam(name = "limit", required = false) Optional<Integer> limitOptional
    ) {
        int offset = offsetOptional.orElse(0);
        int limit = limitOptional.orElse(20);
        LOGGER.info("Search requested (parameters: 'query'=\"" + query + "\", 'site'=\"" + site + "\", 'offset'=" + offset + ", 'limit'=" + limit + ")");
        SearchResponse searchResponse = SEARCH_SERVICE.search(query, site, offset, limit);
        return new ResponseEntity<>(searchResponse.json(), HttpStatus.resolve(searchResponse.getStatusCode()));
    }

    @PostMapping("/indexPage")
    public ResponseEntity<String> indexPage(@RequestParam String url) {
        LOGGER.info("Single page indexing requested (parameters: 'url'=\"" + url + "\")");
        IndexingResponse indexingResponse = INDEXING_SERVICE.indexPage(url);
        return new ResponseEntity<>(indexingResponse.json(), HttpStatus.resolve(indexingResponse.getStatusCode()));
    }
}
