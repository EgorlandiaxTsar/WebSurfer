package com.egorgoncharov.websurfer.dto.indexing;

import com.egorgoncharov.websurfer.services.IndexingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.nodes.Document;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;

public class SiteStructureBuilderWorker extends RecursiveTask<SiteTree> {
    private static final Logger LOGGER = LogManager.getLogger(SiteStructureBuilderWorker.class);
    private final SiteTree parentTree;
    private final IndexingService indexingService;
    private final int requestTimeout;

    public SiteStructureBuilderWorker(SiteTree parentSiteTree, IndexingService indexingService, int requestTimeoutMillis) {
        this.parentTree = parentSiteTree;
        this.indexingService = indexingService;
        this.requestTimeout = requestTimeoutMillis;
    }

    @Override
    protected SiteTree compute() {
        checkState();
        String requestURL = parentTree.getUrl().getProtocol() + "://" + parentTree.getUrl().getHost() + parentTree.getPath();
        List<SiteStructureBuilderWorker> tasks = new ArrayList<>();
        try {
            DocumentResponse documentResponse = PageParser.getDocument(new URL(requestURL));
            Document doc = null;
            if (documentResponse.getResponseCode() >= 200 && documentResponse.getResponseCode() < 300) {
                doc = documentResponse.getDocument();
                List<SiteTree> siteTrees = new ArrayList<>(PageParser.getChildPages(new URL(requestURL), doc));
                siteTrees.forEach(siteTree -> {
                    if (!indexingService.hasPage(siteTree.getUrl().toString() + "/")) {
                        SiteStructureBuilderWorker siteStructureBuilderWorker = new SiteStructureBuilderWorker(siteTree, indexingService, requestTimeout);
                        tasks.add(siteStructureBuilderWorker);
                        siteStructureBuilderWorker.fork();
                    }
                });
            }
            indexingService.addPageData(parentTree.getUrl().getProtocol() + "://" + parentTree.getUrl().getHost(), parentTree.getPath(), documentResponse.getResponseCode(), doc == null ? "" : doc.toString());
            tasks.forEach(task -> parentTree.getChildPages().add(task.join()));
            return parentTree;
        } catch (MalformedURLException e) {
            LOGGER.warn("Failed to index the following page: \"" + requestURL + "\", message=\"" + e.getMessage() + "\"");
        }
        return null;
    }

    public SiteTree getParentPage() {
        return parentTree;
    }

    private void checkState() {
        if (indexingService.getEngineStatus() == IndexingEngineStatus.STOPPING || indexingService.getEngineStatus() == IndexingEngineStatus.STOPPED) {
            throw new IndexingBranchInterruptedException("Force stopping indexing thread \"" + Thread.currentThread().getName() + "\"");
        }
    }
}
