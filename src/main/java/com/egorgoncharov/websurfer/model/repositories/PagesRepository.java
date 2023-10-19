package com.egorgoncharov.websurfer.model.repositories;

import com.egorgoncharov.websurfer.model.entities.PageEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PagesRepository extends CrudRepository<PageEntity, Integer> {
    @Query("select e from PageEntity e where concat(e.site.url, e.path) = ?1")
    PageEntity findPageEntityByUrl(String url);

    @Query("select e from PageEntity e where e.site.id = ?1")
    List<PageEntity> findPageEntitiesBySiteId(int siteId);
}
