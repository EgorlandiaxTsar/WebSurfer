package com.egorgoncharov.websurfer.model.repositories;

import com.egorgoncharov.websurfer.model.entities.SiteEntity;
import com.egorgoncharov.websurfer.model.entities.list.IndexingStatuses;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SitesRepository extends CrudRepository<SiteEntity, Integer> {
    SiteEntity findSiteEntityByUrl(String url);

    List<SiteEntity> findSiteEntitiesByStatus(IndexingStatuses status);
}
