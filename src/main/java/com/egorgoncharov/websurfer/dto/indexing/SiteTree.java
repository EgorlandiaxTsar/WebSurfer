package com.egorgoncharov.websurfer.dto.indexing;

import lombok.Getter;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Getter
public class SiteTree {
    private final URL url;
    private final String path;
    private final List<SiteTree> childPages = new ArrayList<>();

    public SiteTree(URL rootUrl, String path) {
        this.url = rootUrl;
        this.path = path;
    }
}
