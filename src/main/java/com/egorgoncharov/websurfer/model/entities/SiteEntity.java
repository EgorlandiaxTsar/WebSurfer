package com.egorgoncharov.websurfer.model.entities;

import com.egorgoncharov.websurfer.model.entities.list.IndexingStatuses;
import lombok.*;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "site")
@Data
@NoArgsConstructor
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Enumerated(EnumType.STRING)
    private IndexingStatuses status;
    @Column(name = "status_time")
    @Basic
    @Temporal(TemporalType.TIMESTAMP)
    private Date statusTime;
    @Column(name = "last_error")
    private String lastError;
    private String url;
    private String name;
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", insertable = false, updatable = false)
    private List<PageEntity> pages;
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", insertable = false, updatable = false)
    private List<LemmaEntity> lemmas;

    public SiteEntity(int id, IndexingStatuses status, Date statusTime, String lastError, String url, String name) {
        this.id = id;
        this.status = status;
        this.statusTime = statusTime;
        this.lastError = lastError;
        this.url = url;
        this.name = name;
    }
}
