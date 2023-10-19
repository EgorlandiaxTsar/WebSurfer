package com.egorgoncharov.websurfer.services;

import com.egorgoncharov.websurfer.dto.indexing.IndexingEngineStatus;
import com.egorgoncharov.websurfer.dto.indexing.IndexingResponse;

public interface IndexingService {
    IndexingResponse startIndexing();

    IndexingResponse indexPage(String target);

    IndexingResponse stopIndexing();

    void addPageData(String path, String site, int statusCode, String content);

    boolean hasPage(String url);

    IndexingEngineStatus getEngineStatus();
}
