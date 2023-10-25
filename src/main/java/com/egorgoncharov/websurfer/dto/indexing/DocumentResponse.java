package com.egorgoncharov.websurfer.dto.indexing;

import lombok.*;
import org.jsoup.nodes.Document;

@Data
@AllArgsConstructor
public class DocumentResponse {
    private int responseCode;
    private Document document;
}
