package com.egorgoncharov.websurfer.dto.indexing;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SiteTree {
    private final URL URL;
    private final String PATH;
    private final List<SiteTree> CHILD_PAGES = new ArrayList<>();

    public SiteTree(URL rootUrl, String path) {
        this.URL = rootUrl;
        this.PATH = path;
    }

    public URL getUrl() {
        return URL;
    }

    public String getPath() {
        return PATH;
    }

    public List<SiteTree> getChildPages() {
        return CHILD_PAGES;
    }
}
