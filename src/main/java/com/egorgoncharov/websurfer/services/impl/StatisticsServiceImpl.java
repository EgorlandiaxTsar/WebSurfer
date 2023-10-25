package com.egorgoncharov.websurfer.services.impl;

import com.egorgoncharov.websurfer.dto.statistics.DetailedStatisticsItem;
import com.egorgoncharov.websurfer.dto.statistics.StatisticsData;
import com.egorgoncharov.websurfer.dto.statistics.StatisticsResponse;
import com.egorgoncharov.websurfer.dto.statistics.TotalStatistics;
import com.egorgoncharov.websurfer.model.entities.list.IndexingStatuses;
import com.egorgoncharov.websurfer.model.repositories.LemmasRepository;
import com.egorgoncharov.websurfer.model.repositories.PagesRepository;
import com.egorgoncharov.websurfer.model.repositories.SitesRepository;
import com.egorgoncharov.websurfer.services.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private static final Logger LOGGER = LogManager.getLogger(StatisticsServiceImpl.class);
    private final SitesRepository sitesRepository;
    private final PagesRepository pagesRepository;
    private final LemmasRepository lemmasRepository;

    @Override
    public StatisticsResponse getStatistics() {
        LOGGER.info("Preparing statistics");
        StatisticsResponse statisticsResponse = new StatisticsResponse();
        statisticsResponse.setResult(true);
        StatisticsData statisticsData = new StatisticsData();
        statisticsResponse.setStatistics(statisticsData);
        TotalStatistics totalStatistics = new TotalStatistics();
        totalStatistics.setSites((int) sitesRepository.count());
        totalStatistics.setPages((int) pagesRepository.count());
        totalStatistics.setLemmas((int) lemmasRepository.count());
        totalStatistics.setIndexing(!sitesRepository.findSiteEntitiesByStatus(IndexingStatuses.INDEXING).isEmpty());
        statisticsData.setTotal(totalStatistics);
        List<DetailedStatisticsItem> detailedStatisticsItems = new ArrayList<>();
        sitesRepository.findAll().forEach(site -> detailedStatisticsItems.add(new DetailedStatisticsItem(
                site.getUrl(),
                site.getName(),
                site.getStatus().toString().toUpperCase(),
                site.getStatusTime().getTime(),
                site.getLastError(),
                pagesRepository.findPageEntitiesBySiteId(site.getId()).size(),
                lemmasRepository.findLemmaEntitiesBySiteId(site.getId()).size()
        )));
        statisticsData.setDetailed(detailedStatisticsItems);
        LOGGER.info("Statistics model created, releasing");
        return statisticsResponse;
    }
}
