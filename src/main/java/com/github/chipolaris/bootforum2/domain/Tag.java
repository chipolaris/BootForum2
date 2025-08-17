package com.github.chipolaris.bootforum2.domain;

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.persistence.*;

@Entity
@Table(name="TAG_T", uniqueConstraints = {@UniqueConstraint(columnNames="LABEL", name="UNIQ_TAG_LABEL")})
@TableGenerator(name="TagIdGenerator", table="ENTITY_ID_T", pkColumnName="GEN_KEY",
        pkColumnValue="TAG_ID", valueColumnName="GEN_VALUE", initialValue = 1000, allocationSize=10)
@Cacheable(true)
public class Tag extends BaseEntity {

    @PrePersist
    public void prePersist() {
        this.setCreateDate(LocalDateTime.now());
    }

    @PreUpdate
    public void preUpdate() {
        this.setUpdateDate(LocalDateTime.now());
    }

    @Id
    @GeneratedValue(strategy=GenerationType.TABLE, generator="TagIdGenerator")
    private Long id;

    @Column(name="LABEL", length=100, unique = true)
    private String label;

    @Column(name="ICON", length=30)
    private String icon;

    @Column(name="ICON_COLOR", length=30)
    private String iconColor;

    @Column(name="DISABLED")
    private boolean disabled;

    @Column(name="SORT_ORDER")
    private Integer sortOrder;

    @ManyToMany(mappedBy="tags", fetch=FetchType.LAZY)
    private Set<Discussion> discussions;

    @Override
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
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

    public boolean isDisabled() {
        return disabled;
    }
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Set<Discussion> getDiscussions() {
        return discussions;
    }
    public void setDiscussions(Set<Discussion> discussions) {
        this.discussions = discussions;
    }
}