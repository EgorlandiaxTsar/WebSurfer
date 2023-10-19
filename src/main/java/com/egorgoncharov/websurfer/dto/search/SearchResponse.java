package com.egorgoncharov.websurfer.dto.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class SearchResponse {
    private static final Logger LOGGER = LogManager.getLogger(SearchResponse.class);
    private boolean result;
    private int statusCode;
    private int count;
    private List<SearchResponseItem> data;
    private String error;

    public String json() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("result", result);
        if (error != null) {
            root.put("error", error);
            return buildJson(mapper, root);
        }
        root.put("count", count);
        ArrayNode data = mapper.createArrayNode();
        this.data.forEach(e -> {
            ObjectNode dataItem = mapper.createObjectNode();
            dataItem.put("site", e.getSite());
            dataItem.put("siteName", e.getSiteName());
            dataItem.put("uri", e.getUri());
            dataItem.put("title", e.getTitle());
            dataItem.put("snippet", e.getSnippet());
            dataItem.put("relevance", e.getRelevance());
            data.add(dataItem);
        });
        root.put("data", data);
        return buildJson(mapper, root);
    }

    private String buildJson(ObjectMapper mapper, ObjectNode source) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(source);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to build JSON model for search response");
            return null;
        }
    }
}
