package com.egorgoncharov.websurfer.services;

public interface UtilsService {
    boolean compareUrls(String u1, String u2);

    boolean validateUrl(String url);

    boolean siteIncluded(String url);

    String getAbsoluteUrl(String url);

    String getSiteUrl(String url);

    void asyncInvoke(Runnable task);
}
