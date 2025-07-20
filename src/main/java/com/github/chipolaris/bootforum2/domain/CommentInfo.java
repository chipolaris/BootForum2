package com.github.chipolaris.bootforum2.domain;

/**
 * This CommentInfo class is used to keep track of last comment for
 * - UserStat
 * - DiscussionStat
 * - ForumStat
 *
 * It is not used to store the comment itself. The {@link com.github.chipolaris.bootforum2.domain.Comment} does that
 */
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name="COMMENT_INFO_T")
@TableGenerator(name="CommentInfoIdGenerator", table="ENTITY_ID_T", pkColumnName="GEN_KEY",
        pkColumnValue="COMMENT_INFO_ID", valueColumnName="GEN_VALUE", initialValue = 1000, allocationSize=10)
public class CommentInfo extends BaseEntity {

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
    @GeneratedValue(strategy=GenerationType.TABLE, generator="CommentInfoIdGenerator")
    private Long id;

    @Column(name="TITLE", length=255)
    private String title;

    @Column(name="CONTENT_ABBR", length=255)
    private String contentAbbr; // abbreviation of content for display

    @Column(name="COMMENT_ID")
    private Long commentId; // id of the Comment instance

    @Column(name="COMMENTOR", length=50)
    private String commentor;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="COMMENT_DATE")
    private LocalDateTime commentDate;

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

    public Long getCommentId() {
        return commentId;
    }
    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public String getCommentor() {
        return commentor;
    }
    public void setCommentor(String commentor) {
        this.commentor = commentor;
    }

    public LocalDateTime getCommentDate() {
        return commentDate;
    }
    public void setCommentDate(LocalDateTime commentDate) {
        this.commentDate = commentDate;
    }
}