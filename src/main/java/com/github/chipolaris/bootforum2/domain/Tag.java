package com.github.chipolaris.bootforum2.domain;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

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

    @Column(name="COLOR", length=30)
    private String color;

    @Column(name="DISABLED")
    private boolean disabled;

    @Column(name="SORT_ORDER")
    private Integer sortOrder;

    @Transient
    private List<Discussion> discussions;

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

    public String getColor() {
        return color;
    }
    public void setColor(String color) {
        this.color = color;
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

    public List<Discussion> getDiscussions() {
        return discussions;
    }
    public void setDiscussions(List<Discussion> discussions) {
        this.discussions = discussions;
    }
}