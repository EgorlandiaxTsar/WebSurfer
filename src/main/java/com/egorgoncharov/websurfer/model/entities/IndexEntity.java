package com.egorgoncharov.websurfer.model.entities;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "`index`")
@Data
@NoArgsConstructor
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", insertable = false, updatable = false)
    private PageEntity page;
    @Column(name = "page_id")
    private int pageID;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", insertable = false, updatable = false)
    private LemmaEntity lemma;
    @Column(name = "lemma_id")
    private int lemmaID;
    @Column(name = "`rank`")
    private float rank;

    public IndexEntity(int id, PageEntity page, LemmaEntity lemma, float rank) {
        this.id = id;
        this.page = page;
        this.pageID = page.getId();
        this.lemma = lemma;
        this.lemmaID = lemma.getId();
        this.rank = rank;
    }
}
