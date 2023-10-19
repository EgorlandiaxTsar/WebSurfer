package com.egorgoncharov.websurfer.model.repositories;

import com.egorgoncharov.websurfer.model.entities.LemmaEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LemmasRepository extends CrudRepository<LemmaEntity, Integer> {
    @Query("select e from LemmaEntity e where e.lemma = ?1")
    List<LemmaEntity> findLemmasByLemma(String lemma);

    @Query("select e from LemmaEntity e where e.lemma = ?1 and e.site.url = ?2")
    LemmaEntity findLemmasByLemmaAndSiteUrl(String lemma, String siteUrl);

    @Query("select e from LemmaEntity e where e.site.id = ?1")
    List<LemmaEntity> findLemmaEntitiesBySiteId(int siteId);
}
