package com.egorgoncharov.websurfer.services;

import com.egorgoncharov.websurfer.dto.search.SearchResponse;

public interface SearchService {
    SearchResponse search(String query, String site, int offset, int limit);
}
