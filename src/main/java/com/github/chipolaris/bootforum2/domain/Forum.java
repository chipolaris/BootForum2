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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

@Entity
@Table(name="FORUM_T")
@TableGenerator(name="ForumIdGenerator", table="ENTITY_ID_T", pkColumnName="GEN_KEY",
        pkColumnValue="FORUM_ID", valueColumnName="GEN_VALUE", initialValue = 1000, allocationSize=10)
public class Forum extends BaseEntity {

    @PrePersist
    public void prePersist() {
        this.setStat(new ForumStat());
        this.setCreateDate(LocalDateTime.now());
    }

    @PreUpdate
    public void preUpdate() {
        this.setUpdateDate(LocalDateTime.now());
    }

    @Id
    @GeneratedValue(strategy=GenerationType.TABLE, generator="ForumIdGenerator")
    private Long id;

    @Column(name="TITLE", length=100)
    private String title;

    @Column(name="description", length=255)
    private String description;

    // icon to display
    @Column(name="ICON", length=50)
    private String icon;

    // icon iconColor to display
    @Column(name="ICON_COLOR", length=50)
    private String iconColor;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="FORUM_GROUP_ID", foreignKey = @ForeignKey(name="FK_FORUM_FORUM_GROUP"))
    private ForumGroup forumGroup; // point to the ForumGroup that contains this

    @OneToMany(fetch=FetchType.LAZY, mappedBy="forum")
    private List<Discussion> discussions;

    @OneToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="FORUM_STAT_ID", foreignKey = @ForeignKey(name="FK_FORUM_FORUM_STAT"))
    private ForumStat stat;

    @Column(name="ACTIVE")
    private boolean active;

    @Column(name="SORT_ORDER")
    private Integer sortOrder;

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

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }
    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIconColor() {
        return iconColor;
    }
    public void setIconColor(String iconColor) {
        this.iconColor = iconColor;
    }

    public ForumGroup getForumGroup() {
        return forumGroup;
    }
    public void setForumGroup(ForumGroup forumGroup) {
        this.forumGroup = forumGroup;
    }

    public List<Discussion> getDiscussions() {
        return discussions;
    }
    public void setDiscussions(List<Discussion> discussions) {
        this.discussions = discussions;
    }

    public ForumStat getStat() {
        return stat;
    }
    public void setStat(ForumStat stat) {
        this.stat = stat;
    }

    public boolean isActive() {
        return active;
    }
    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}