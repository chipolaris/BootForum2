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
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

@Entity
@Table(name="FORUM_GROUP_T")
@TableGenerator(name="ForumGroupIdGenerator", table="ENTITY_ID_T", pkColumnName="GEN_KEY",
        pkColumnValue="FORUM_GROUP_ID", valueColumnName="GEN_VALUE", initialValue = 1000, allocationSize=10)
public class ForumGroup extends BaseEntity {

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if(this.getCreateDate() == null) {
            this.setCreateDate(now);
        }
        if(this.getUpdateDate() == null) {
            this.setUpdateDate(now);
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.setUpdateDate(LocalDateTime.now());
    }

    @Id
    @GeneratedValue(strategy=GenerationType.TABLE, generator="ForumGroupIdGenerator")
    private Long id;

    @Column(name="TITLE", length=100)
    private String title;

    // icon to display
    @Column(name="ICON", length=50)
    private String icon;

    // icon iconColor to display
    @Column(name="ICON_COLOR", length=50)
    private String iconColor;

    /**
     * Note: set cascade to REMOVE to enable automatic removal of associated Forums 
     */
    @OneToMany(cascade=CascadeType.REMOVE, fetch=FetchType.LAZY, mappedBy="forumGroup")
    //@OrderColumn(name="SORT_ORDER") // note: this SORT_ORDER column is in Forum table
    @OrderBy("sortOrder")
    private List<Forum> forums; // use List instead of Set to sort

    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="PARENT_ID", foreignKey = @ForeignKey(name="FK_FORUM_GROUP_PARENT"))
    private ForumGroup parent;

    /**
     * Note: set cascade to REMOVE to enable automatic removal of sub ForumGroups 
     */
    @OneToMany(cascade=CascadeType.REMOVE, fetch=FetchType.LAZY, mappedBy="parent")
    //@OrderColumn(name="SORT_ORDER") // note: this SORT_ORDER column is in ForumGroup table
    @OrderBy("sortOrder")
    private List<ForumGroup> subGroups; // use List instead of Set to sort

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

    public List<Forum> getForums() {
        return forums;
    }
    public void setForums(List<Forum> forums) {
        this.forums = forums;
    }

    public ForumGroup getParent() {
        return parent;
    }
    public void setParent(ForumGroup parent) {
        this.parent = parent;
    }

    public List<ForumGroup> getSubGroups() {
        return subGroups;
    }
    public void setSubGroups(List<ForumGroup> subGroups) {
        this.subGroups = subGroups;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}