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
    private final SiteTree PARENT_TREE;
    private final IndexingService INDEXING_SERVICE;
    private final int REQ_TIMEOUT_MILLIS;

    public SiteStructureBuilderWorker(SiteTree parentSiteTree, IndexingService indexingService, int requestTimeoutMillis) {
        this.PARENT_TREE = parentSiteTree;
        this.INDEXING_SERVICE = indexingService;
        this.REQ_TIMEOUT_MILLIS = requestTimeoutMillis;
    }

    @Override
    protected SiteTree compute() {
        checkState();
        String requestURL = PARENT_TREE.getUrl().getProtocol() + "://" + PARENT_TREE.getUrl().getHost() + PARENT_TREE.getPath();
        List<SiteStructureBuilderWorker> tasks = new ArrayList<>();
        try {
            DocumentResponse documentResponse = PageParser.getDocument(new URL(requestURL));
            Document doc = null;
            if (documentResponse.getResponseCode() >= 200 && documentResponse.getResponseCode() < 300) {
                doc = documentResponse.getDocument();
                List<SiteTree> siteTrees = new ArrayList<>(PageParser.getChildPages(new URL(requestURL), doc));
                siteTrees.forEach(siteTree -> {
                    if (!INDEXING_SERVICE.hasPage(siteTree.getUrl().toString() + "/")) {
                        SiteStructureBuilderWorker siteStructureBuilderWorker = new SiteStructureBuilderWorker(siteTree, INDEXING_SERVICE, REQ_TIMEOUT_MILLIS);
                        tasks.add(siteStructureBuilderWorker);
                        siteStructureBuilderWorker.fork();
                    }
                });
            }
            INDEXING_SERVICE.addPageData(PARENT_TREE.getUrl().getProtocol() + "://" + PARENT_TREE.getUrl().getHost(), PARENT_TREE.getPath(), documentResponse.getResponseCode(), doc == null ? "" : doc.toString());
            tasks.forEach(task -> PARENT_TREE.getChildPages().add(task.join()));
            return PARENT_TREE;
        } catch (MalformedURLException e) {
            LOGGER.warn("Failed to index the following page: \"" + requestURL + "\", message=\"" + e.getMessage() + "\"");
        }
        return null;
    }

    public SiteTree getParentPage() {
        return PARENT_TREE;
    }

    private void checkState() {
        if (INDEXING_SERVICE.getEngineStatus() == IndexingEngineStatus.STOPPING || INDEXING_SERVICE.getEngineStatus() == IndexingEngineStatus.STOPPED) {
            throw new IndexingBranchInterruptedException("Force stopping indexing thread \"" + Thread.currentThread().getName() + "\"");
        }
    }
}
