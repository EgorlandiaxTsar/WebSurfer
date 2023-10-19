package com.egorgoncharov.websurfer.dto.indexing;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.jsoup.nodes.Document;

@AllArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public class DocumentResponse {
    private int responseCode;
    private Document document;
}
