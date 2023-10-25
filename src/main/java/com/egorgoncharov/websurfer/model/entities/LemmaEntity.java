package com.egorgoncharov.websurfer.model.entities;

import lombok.*;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "lemma")
@Data
@NoArgsConstructor
public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", insertable = false, updatable = false)
    private SiteEntity site;
    @Column(name = "site_id")
    private int siteID;
    private String lemma;
    private int frequency;
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", insertable = false, updatable = false)
    private List<IndexEntity> indexes;

    public LemmaEntity(int id, SiteEntity site, String lemma, int frequency) {
        this.id = id;
        this.site = site;
        this.siteID = site.getId();
        this.lemma = lemma;
        this.frequency = frequency;
    }
}
