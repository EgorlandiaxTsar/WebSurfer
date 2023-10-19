package com.egorgoncharov.websurfer.model.repositories;

import com.egorgoncharov.websurfer.model.entities.SiteEntity;
import com.egorgoncharov.websurfer.model.entities.list.IndexingStatuses;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface SitesRepository extends CrudRepository<SiteEntity, Integer> {
    SiteEntity findSiteEntityByUrl(String url);

    List<SiteEntity> findSiteEntitiesByStatus(IndexingStatuses status);

    @Transactional
    @Modifying
    @Query("delete from SiteEntity e where e.url = ?1")
    void deleteBySiteUrl(String siteUrl);
}
