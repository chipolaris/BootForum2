package com.github.chipolaris.bootforum2.domain;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

@Entity
@Table(name="DISCUSSION_T")
@TableGenerator(name="DiscussionIdGenerator", table="ENTITY_ID_T", pkColumnName="GEN_KEY",
        pkColumnValue="DISCUSSION_ID", valueColumnName="GEN_VALUE", initialValue = 1000, allocationSize=10)
public class Discussion extends BaseEntity {

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.setCreateDate(now);
        this.setUpdateDate(now);
    }

    @PreUpdate
    public void preUpdate() {
        this.setUpdateDate(LocalDateTime.now());
    }

    @Id
    @GeneratedValue(strategy=GenerationType.TABLE, generator="DiscussionIdGenerator")
    private Long id;

    @Column(name="TITLE", length=255, nullable=false)
    private String title;

    @Column(name="CLOSED")
    private boolean closed;

    @Column(name="STICKY")
    private boolean sticky;

    @Column(name="IMPORTANT")
    private boolean important;

    @OneToMany(fetch=FetchType.LAZY, mappedBy="discussion", cascade=CascadeType.ALL)
    @OrderBy("id ASC")
    private List<Comment> comments; // all comments for this discussion thread

    @OneToOne(fetch=FetchType.EAGER, cascade=CascadeType.ALL)
    @JoinColumn(name="DISCUSSION_STAT_ID")
    private DiscussionStat stat;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="FORUM_ID", foreignKey=@ForeignKey(name="FK_DISCUSSION_FORUM"))
    private Forum forum;

    @ManyToMany(fetch=FetchType.EAGER)
    @JoinTable(name="DISCUSSION_TAG_T",
            joinColumns={@JoinColumn(name="DISCUSSION_ID", foreignKey = @ForeignKey(name="FK_DISCUS_TAG_DISCUSSION"))},
            inverseJoinColumns={@JoinColumn(name="TAG_ID", foreignKey = @ForeignKey(name="FK_DISCUS_TAG_TAG"))},
            indexes = {@Index(name="IDX_DISCUS_TAG", columnList = "DISCUSSION_ID,TAG_ID")})
    private List<Tag> tags;

    @Override
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isClosed() {
        return closed;
    }
    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public boolean isSticky() {
        return sticky;
    }
    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }

    public boolean isImportant() {
        return important;
    }
    public void setImportant(boolean important) {
        this.important = important;
    }

    public List<Comment> getComments() {
        return comments;
    }
    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public DiscussionStat getStat() {
        return stat;
    }
    public void setStat(DiscussionStat stat) {
        this.stat = stat;
    }

    public Forum getForum() {
        return forum;
    }
    public void setForum(Forum forum) {
        this.forum = forum;
    }

    public List<Tag> getTags() {
        return tags;
    }
    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }
}