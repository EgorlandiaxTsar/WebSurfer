package com.egorgoncharov.websurfer.model.repositories;

import com.egorgoncharov.websurfer.model.entities.IndexEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IndexesRepository extends CrudRepository<IndexEntity, Integer> {
    @Query("select e from IndexEntity e where e.lemma.lemma = ?1")
    List<IndexEntity> findIndexesByLemma(String lemma);

    @Query("select e from IndexEntity e where e.lemma.lemma = ?1 and e.lemma.site.url = ?2")
    List<IndexEntity> findIndexesByLemmaAndSiteUrl(String lemma, String siteUrl);
}
