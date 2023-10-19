package com.egorgoncharov.websurfer.services.impl;

import com.egorgoncharov.websurfer.config.Site;
import com.egorgoncharov.websurfer.config.SitesList;
import com.egorgoncharov.websurfer.dto.indexing.*;
import com.egorgoncharov.websurfer.model.entities.IndexEntity;
import com.egorgoncharov.websurfer.model.entities.LemmaEntity;
import com.egorgoncharov.websurfer.model.entities.PageEntity;
import com.egorgoncharov.websurfer.model.entities.SiteEntity;
import com.egorgoncharov.websurfer.model.entities.list.IndexingStatuses;
import com.egorgoncharov.websurfer.model.repositories.IndexesRepository;
import com.egorgoncharov.websurfer.model.repositories.LemmasRepository;
import com.egorgoncharov.websurfer.model.repositories.PagesRepository;
import com.egorgoncharov.websurfer.model.repositories.SitesRepository;
import com.egorgoncharov.websurfer.services.IndexingService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.PessimisticLockException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private static final Logger LOGGER = LogManager.getLogger(IndexingServiceImpl.class);
    private final List<IndexingForkJoinPoolRunner> INITIAL_THREADS = new ArrayList<>();
    private final AtomicReference<IndexingEngineStatus> ENGINE_STATUS = new AtomicReference<>(IndexingEngineStatus.STOPPED);
    private final MorphologyServiceImpl MORPHOLOGY_SERVICE = MorphologyServiceImpl.getInstance();
    private final UtilsServiceImpl UTILS;
    private final SitesList SITES_LIST;
    private final SitesRepository SITES_REPOSITORY;
    private final PagesRepository PAGES_REPOSITORY;
    private final LemmasRepository LEMMAS_REPOSITORY;
    private final IndexesRepository INDEXES_REPOSITORY;
    private boolean blockRequests = false;
    private int indexingsEnded = 0;

    @Override
    public IndexingResponse startIndexing() {
        if (blockRequests) {
            LOGGER.warn("Indexing start rejected: 429, Requests overload");
            return new IndexingResponse(false, 429, "Слишком много запросов");
        }
        if (ENGINE_STATUS.get() == IndexingEngineStatus.STOPPING) {
            LOGGER.warn("Indexing start rejected: 400, Indexing is stopping");
            return new IndexingResponse(false, 400, "Индексация останавливается");
        }
        if (ENGINE_STATUS.get() == IndexingEngineStatus.STARTING) {
            LOGGER.warn("Indexing start rejected: 400, Indexing is starting");
            return new IndexingResponse(false, 400, "Индексация запускается");
        }
        if (ENGINE_STATUS.get() == IndexingEngineStatus.STARTED) {
            LOGGER.warn("Indexing start rejected: 400, Indexing is already started");
            return new IndexingResponse(false, 400, "Индексация уже запущена");
        }
        blockRequests();
        LOGGER.info("Indexing is starting");
        long start = System.currentTimeMillis();
        switchEngineStatus(IndexingEngineStatus.STARTING);
        UTILS.asyncInvoke(() -> {
            clearRepositories();
            beginIndexing();
            LOGGER.info("Indexing successfully started in " + (System.currentTimeMillis() + start) + "ms");
            switchEngineStatus(IndexingEngineStatus.STARTED);
        });
        return new IndexingResponse(true, 200);
    }

    @Override
    public IndexingResponse indexPage(String target) {
        if (blockRequests) {
            LOGGER.warn("Single page indexing start rejected: 429, Requests overload");
            return new IndexingResponse(false, 429, "Слишком много запросов");
        }
        if (!UTILS.validateUrl(target)) {
            LOGGER.warn("Single page indexing start rejected: 400, Incorrect URL");
            return new IndexingResponse(false, 400, "Некорректный URL-Адрес");
        }
        if (!UTILS.siteIncluded(target)) {
            LOGGER.warn("Single page indexing start rejected: 404, Page not found");
            return new IndexingResponse(false, 404, "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }
        blockRequests();
        LOGGER.info("Single page indexing is starting");
        long start = System.currentTimeMillis();
        UTILS.asyncInvoke(() -> {
            clearPageRepository(target);
            beginIndexing(target);
            LOGGER.info("Single page indexing is started in " + (System.currentTimeMillis() - start) + "ms");
        });
        return new IndexingResponse(true, 200);
    }

    @Override
    public IndexingResponse stopIndexing() {
        if (blockRequests) {
            LOGGER.warn("Indexing stop rejected: 429, Requests overload");
            return new IndexingResponse(false, 429, "Слишком много запросов");
        }
        if (ENGINE_STATUS.get() == IndexingEngineStatus.STARTING) {
            LOGGER.warn("Indexing stop rejected: 400, Indexing is starting");
            return new IndexingResponse(false, 400, "Индексация запускается");
        }
        if (ENGINE_STATUS.get() == IndexingEngineStatus.STOPPING) {
            LOGGER.warn("Indexing stop rejected: 400, Indexing is stopping");
            return new IndexingResponse(false, 400, "Индексация останавливается");
        }
        if (ENGINE_STATUS.get() == IndexingEngineStatus.STOPPED) {
            LOGGER.warn("Indexing stop rejected: 400, Indexing is not started");
            return new IndexingResponse(false, 400, "Индексация не запущенна");
        }
        blockRequests();
        LOGGER.info("Indexing is stopping");
        long start = System.currentTimeMillis();
        switchEngineStatus(IndexingEngineStatus.STOPPING);
        UTILS.asyncInvoke(() -> {
            INITIAL_THREADS.forEach(Thread::interrupt);
            List<SiteEntity> indexingSites = new ArrayList<>(SITES_REPOSITORY.findSiteEntitiesByStatus(IndexingStatuses.INDEXING));
            indexingSites.forEach(indexingSite -> {
                indexingSite.setStatus(IndexingStatuses.FAILED);
                indexingSite.setLastError("Индексация остановлена пользователем");
                indexingSite.setStatusTime(new Date());
            });
            SITES_REPOSITORY.saveAll(indexingSites);
            INITIAL_THREADS.clear();
            LOGGER.info("Indexing stopped in " + (System.currentTimeMillis() - start) + "ms");
            switchEngineStatus(IndexingEngineStatus.STOPPED);
        });
        return new IndexingResponse(true, 200);
    }

    @Override
    public synchronized void addPageData(String site, String path, int statusCode, String content) {
        LOGGER.info("Adding page data (parameters: 'site'=\"" + site + "\", 'path'=\"" + path + "\", 'statusCode'=" + statusCode + ", 'content'=\"" + "...\")");
        SiteEntity siteEntity = SITES_REPOSITORY.findSiteEntityByUrl(site);
        PageEntity page = PAGES_REPOSITORY.save(new PageEntity(0, siteEntity, path, statusCode, content));
        if (!content.isEmpty()) {
            updateLemmasAndIndexes(siteEntity, page, content);
        }
    }

    @Override
    public boolean hasPage(String url) {
        return PAGES_REPOSITORY.findPageEntityByUrl(url) != null;
    }

    public void addOrUpdatePageData(String site, String path, int statusCode, String content) {
        LOGGER.info("Adding/Updating page data (parameters: 'site'=\"" + site + "\", 'path'=\"" + path + "\", 'statusCode'=" + statusCode + ", 'content'=\"" + "...\")");
        SiteEntity siteEntity = SITES_REPOSITORY.findSiteEntityByUrl(site);
        PageEntity page = PAGES_REPOSITORY.findPageEntityByUrl(UTILS.getAbsoluteUrl(site + path) + "/");
        if (page == null) {
            page = PAGES_REPOSITORY.save(new PageEntity(0, siteEntity, path, statusCode, content));
        }
        if (!content.isEmpty()) {
            updateLemmasAndIndexes(siteEntity, page, content);
        }
    }

    @Override
    public IndexingEngineStatus getEngineStatus() {
        return ENGINE_STATUS.get();
    }

    private void switchEngineStatus(IndexingEngineStatus engineStatus) {
        this.ENGINE_STATUS.set(engineStatus);
    }

    private void blockRequests() {
        if (!blockRequests) {
            blockRequests = true;
            UTILS.asyncInvoke(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted exception in requests overload handler detected, message=\"" + e.getMessage() + "\"");
                }
                blockRequests = false;
            });
        }
    }

    private void reportIndexingEnd(String site) {
        LOGGER.info("Site \"" + site + "\" has been indexed");
        indexingsEnded++;
        if (indexingsEnded == SITES_LIST.getSites().size()) {
            indexingsEnded = 0;
            stopIndexing();
        }
    }

    private void clearRepositories() {
        try {
            SITES_REPOSITORY.deleteAll();
            PAGES_REPOSITORY.deleteAll();
            LEMMAS_REPOSITORY.deleteAll();
            INDEXES_REPOSITORY.deleteAll();
        } catch (PessimisticLockException e) {
            LOGGER.error("Pessimistic Lock exception while clearing repositories detected (too much time spent on clearing data), message=\"" + e.getMessage() + "\"");
        }
    }

    private void clearPageRepository(String target) {
        target = UTILS.getAbsoluteUrl(target);
        PageEntity page = PAGES_REPOSITORY.findPageEntityByUrl(target);
        if (page == null) return;
        List<IndexEntity> indexesToDelete = new ArrayList<>();
        List<LemmaEntity> lemmasToDelete = new ArrayList<>();
        page.getIndexes().forEach(index -> {
            indexesToDelete.add(index);
            lemmasToDelete.add(index.getLemma());
        });
        LEMMAS_REPOSITORY.deleteAll(lemmasToDelete);
        INDEXES_REPOSITORY.deleteAll(indexesToDelete);
        PAGES_REPOSITORY.delete(page);
    }

    private void beginIndexing() {
        List<SiteEntity> sitesSQLEntities = new ArrayList<>();
        SITES_LIST.getSites().forEach(initialSite -> {
            sitesSQLEntities.add(new SiteEntity(0, IndexingStatuses.INDEXING, new Date(), null, initialSite.getUrl(), initialSite.getName()));
            ForkJoinPool siteIndexingTasks = new ForkJoinPool();
            INITIAL_THREADS.add(new IndexingForkJoinPoolRunner(() -> {
                setupSiteIndexingThread(initialSite, siteIndexingTasks);
                reportIndexingEnd(initialSite.getUrl());
            }, siteIndexingTasks));
        });
        SITES_REPOSITORY.saveAll(sitesSQLEntities);
        INITIAL_THREADS.forEach(Thread::start);
    }

    private void beginIndexing(String target) {
        target = UTILS.getAbsoluteUrl(target) + "/";
        try {
            URL targetUrl = new URL(target);
            String requestURL = targetUrl.getProtocol() + "://" + targetUrl.getHost() + targetUrl.getPath();
            DocumentResponse documentResponse = PageParser.getDocument(new URL(requestURL));
            Document doc = null;
            if (documentResponse.getResponseCode() >= 200 && documentResponse.getResponseCode() < 300) {
                doc = documentResponse.getDocument();
            }
            addOrUpdatePageData(targetUrl.getProtocol() + "://" + targetUrl.getHost(), targetUrl.getPath(), documentResponse.getResponseCode(), doc == null ? "" : doc.toString());
        } catch (MalformedURLException e) {
            LOGGER.warn("Failed to index the following page: \"" + target + "\", message=\"" + e.getMessage() + "\"");
        }
    }

    private void setupSiteIndexingThread(Site targetSite, ForkJoinPool taskPool) {
        SiteEntity siteSQLEntity = SITES_REPOSITORY.findSiteEntityByUrl(targetSite.getUrl());
        try {
            SiteTree siteTree = taskPool.invoke(
                    new SiteStructureBuilderWorker(
                            new SiteTree(
                                    new URL(targetSite.getUrl()),
                                    "/"),
                            this,
                            targetSite.getRequestTimeout()
                    )
            );
            if (siteTree.getChildPages().isEmpty()) {
                siteSQLEntity.setStatus(IndexingStatuses.FAILED);
                siteSQLEntity.setLastError("Главная страница сайта не доступна");
            } else {
                siteSQLEntity.setStatus(IndexingStatuses.INDEXED);
            }
            siteSQLEntity.setStatusTime(new Date());
            SITES_REPOSITORY.save(siteSQLEntity);
        } catch (IndexingBranchInterruptedException e) {
            System.out.println(e.getMessage());
            LOGGER.warn("Forced termination of indexing branch, message=\"" + e.getMessage() + "\"");
        } catch (MalformedURLException e) {
            System.err.println("Failed to start indexing the following site: " + siteSQLEntity.getUrl() + " ('" + siteSQLEntity.getName() + "'): " + e.getMessage());
            LOGGER.warn("Failed to index the following site: \"" + siteSQLEntity.getUrl() + "\", message=\"" + e.getMessage() + "\"");
            siteSQLEntity.setStatus(IndexingStatuses.FAILED);
            siteSQLEntity.setLastError("Некорректный URL");
            siteSQLEntity.setStatusTime(new Date());
            SITES_REPOSITORY.save(siteSQLEntity);
        } catch (NullPointerException e) {
            LOGGER.error("Unexpected Null Pointer exception detected, message=\"" + e.getMessage() + "\"");
        }
    }

    private synchronized void updateLemmasAndIndexes(SiteEntity site, PageEntity page, String content) {
        String docTextContent = MORPHOLOGY_SERVICE.getPageText(Jsoup.parse(content));
        Map<String, Integer> lemmaFrequency = new HashMap<>(MORPHOLOGY_SERVICE.getLemmasStatistics(docTextContent));
        List<LemmaEntity> lemmaEntities = new ArrayList<>();
        List<IndexEntity> indexEntities = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : lemmaFrequency.entrySet()) {
            String key = entry.getKey();
            Integer freq = entry.getValue();
            LemmaEntity lemmaEntity = new LemmaEntity(0, site, key, 1);
            try {
                LemmaEntity availableLemma = LEMMAS_REPOSITORY.findLemmasByLemmaAndSiteUrl(key, site.getUrl());
                if (availableLemma != null) {
                    lemmaEntity.setId(availableLemma.getId());
                    lemmaEntity.setFrequency(availableLemma.getFrequency() + 1);
                }
                lemmaEntities.add(lemmaEntity);
                indexEntities.add(new IndexEntity(0, page, lemmaEntity, freq));
            } catch (IncorrectResultSizeDataAccessException e) {
                LOGGER.error("Incorrect Result Size Data Access exception detected (database returned multiple results when single result was expected), message=\"" + e.getMessage() + "\"");
            }
        }
        List<LemmaEntity> addedLemmas = new ArrayList<>((Collection<LemmaEntity>) LEMMAS_REPOSITORY.saveAll(lemmaEntities));
        for (int i = 0; i < addedLemmas.size(); i++) {
            LemmaEntity lemma = addedLemmas.get(i);
            indexEntities.get(i).setLemma(lemma);
            indexEntities.get(i).setLemmaID(lemma.getId());
        }
        INDEXES_REPOSITORY.saveAll(indexEntities);
    }
}

class IndexingForkJoinPoolRunner extends Thread {
    private final ForkJoinPool TASKS;

    public IndexingForkJoinPoolRunner(Runnable target, ForkJoinPool tasks) {
        super(target);
        this.TASKS = tasks;
    }

    public ForkJoinPool getTasks() {
        return TASKS;
    }
}
