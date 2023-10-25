package com.egorgoncharov.websurfer.services.impl;

import com.egorgoncharov.websurfer.config.Site;
import com.egorgoncharov.websurfer.config.SitesList;
import com.egorgoncharov.websurfer.services.UtilsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;

@Service
@RequiredArgsConstructor
public class UtilsServiceImpl implements UtilsService {
    private final SitesList sitesList;

    @Override
    public boolean compareUrls(String u1, String u2) {
        return getAbsoluteUrl(u1).equals(getAbsoluteUrl(u2));
    }

    @Override
    public boolean validateUrl(String url) {
        url = getAbsoluteUrl(url);
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    @Override
    public boolean siteIncluded(String url) {
        String host = getAbsoluteUrl(url.replaceFirst("https://", "").replaceFirst("http://", "").split("/")[0]);
        for (Site site : sitesList.getSites()) {
            if (compareUrls(site.getUrl(), host)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getAbsoluteUrl(String url) {
        url = url.replaceFirst("www\\.", "");
        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            url = "https://" + url;
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url.toLowerCase();
    }

    @Override
    public String getSiteUrl(String url) {
        return getAbsoluteUrl(url.replaceFirst("https://", "").replaceFirst("http://", "").split("/")[0]);
    }

    @Override
    public void asyncInvoke(Runnable task) {
        new Thread(task).start();
    }
}
