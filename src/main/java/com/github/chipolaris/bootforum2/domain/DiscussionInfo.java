package com.github.chipolaris.bootforum2.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="DISCUSSION_INFO_T")
@TableGenerator(name="DiscussionInfoIdGenerator", table="ENTITY_ID_T", pkColumnName="GEN_KEY",
        pkColumnValue="DISCUSSION_INFO_ID", valueColumnName="GEN_VALUE", initialValue = 1000, allocationSize=10)
public class DiscussionInfo extends BaseEntity {

    private static final int CONTENT_ABBR_MAX_LENGTH = 255;

    @PrePersist
    public void prePersist() {
        abbreviateContent();
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
        abbreviateContent();
        this.setUpdateDate(LocalDateTime.now());
    }

    private void abbreviateContent() {
        if(this.contentAbbr != null && this.contentAbbr.length() > CONTENT_ABBR_MAX_LENGTH) {
            this.contentAbbr = this.contentAbbr.substring(0, CONTENT_ABBR_MAX_LENGTH - 3) + "...";
        }
    }

    @Id
    @GeneratedValue(strategy=GenerationType.TABLE, generator="DiscussionInfoIdGenerator")
    private Long id;

    @Column(name="TITLE", length=255)
    private String title;

    @Column(name="CONTENT_ABBR", length=255)
    private String contentAbbr; // abbreviation of content for display

    @Column(name="DISCUSSION_ID")
    private Long discussionId; // id of the Discussion instance

    @Column(name="DISCUSSION_CREATOR", length=50)
    private String discussionCreator;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="DISCUSSION_CREATE_DATE")
    private LocalDateTime discussionCreateDate;

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

    public String getContentAbbr() {
        return contentAbbr;
    }
    public void setContentAbbr(String contentAbbr) {
        this.contentAbbr = contentAbbr;
    }

    public Long getDiscussionId() {
        return discussionId;
    }
    public void setDiscussionId(Long discussionId) {
        this.discussionId = discussionId;
    }

    public String getDiscussionCreator() {
        return discussionCreator;
    }
    public void setDiscussionCreator(String discussionCreator) {
        this.discussionCreator = discussionCreator;
    }

    public LocalDateTime getDiscussionCreateDate() {
        return discussionCreateDate;
    }
    public void setDiscussionCreateDate(LocalDateTime discussionCreateDate) {
        this.discussionCreateDate = discussionCreateDate;
    }
}