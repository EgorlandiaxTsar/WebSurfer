package com.egorgoncharov.websurfer.dto.indexing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Data
public class IndexingResponse {
    private static final Logger LOGGER = LogManager.getLogger(IndexingResponse.class);
    private boolean result;
    private int statusCode;
    private String error = null;

    public IndexingResponse(boolean result, int statusCode, String error) {
        this.result = result;
        this.statusCode = statusCode;
        this.error = error;
    }

    public IndexingResponse(boolean result, int statusCode) {
        this.result = result;
        this.statusCode = statusCode;
    }

    public String json() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("result", result);
        if (error != null) root.put("error", error);
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to build JSON model for indexing response");
            return null;
        }
    }
}
