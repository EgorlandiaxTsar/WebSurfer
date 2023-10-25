package com.egorgoncharov.websurfer.dto.search;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResponseItem {
    private String site;
    private String siteName;
    private String uri;
    private String title;
    private String snippet;
    private double relevance;
}
