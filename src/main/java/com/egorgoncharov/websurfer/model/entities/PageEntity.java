package com.egorgoncharov.websurfer.model.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "page")
@NoArgsConstructor
@Getter
@Setter
@ToString
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", insertable = false, updatable = false)
    private SiteEntity site;
    @Column(name = "site_id")
    private int siteID;
    private String path;
    private int code;
    private String content;
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", insertable = false, updatable = false)
    private List<IndexEntity> indexes;

    public PageEntity(int id, SiteEntity site, String path, int code, String content) {
        this.id = id;
        this.site = site;
        this.siteID = site.getId();
        this.path = path;
        this.code = code;
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PageEntity)) return false;
        if (((PageEntity) o).getSite() == null || ((PageEntity) o).getPath() == null) return false;
        if (((PageEntity) o).getId() <= 0) {
            return this.getSite().getUrl().equals(((PageEntity) o).getSite().getUrl()) && this.getPath().equals(((PageEntity) o).getPath());
        } else {
            return this.getId() == ((PageEntity) o).getId();
        }
    }
}
