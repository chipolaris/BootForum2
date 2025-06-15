package com.github.chipolaris.bootforum2.domain;

import java.time.LocalDateTime;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name="DISCUSSION_STAT_T")
@TableGenerator(name="DiscussionStatIdGenerator", table="ENTITY_ID_T", pkColumnName="GEN_KEY",
        pkColumnValue="DISCUSSION_STAT_ID", valueColumnName="GEN_VALUE", initialValue = 1000, allocationSize=10)
public class DiscussionStat extends BaseEntity {

    @Id
    @GeneratedValue(strategy=GenerationType.TABLE, generator="DiscussionStatIdGenerator")
    private Long id;

    @OneToOne(fetch=FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name="LAST_COMMENT_INFO_ID", foreignKey = @ForeignKey(name="FK_DISC_STAT_LAST_COMMEN"))
    private CommentInfo lastComment; // last comment of this discussion

    @Column(name="COMMENT_COUNT")
    private long commentCount;

    @Column(name="VIEW_COUNT")
    private long viewCount;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="LAST_VIEWED")
    private LocalDateTime lastViewed;

    @Column(name="THUMBNAIL_COUNT")
    private long thumbnailCount;

    @Column(name="ATTACHMENT_COUNT")
    private long attachmentCount;

    @ElementCollection
    @CollectionTable(name = "DISC_STAT_PARTICIPANT_T",
            joinColumns = {@JoinColumn(name = "DISC_STAT_ID")})
    @MapKeyColumn(name = "COMMENTOR")
    @Column(name = "COMMENT_COUNT")
    private Map<String, Integer> participants;

    @Override
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public CommentInfo getLastComment() {
        return lastComment;
    }
    public void setLastComment(CommentInfo lastComment) {
        this.lastComment = lastComment;
    }

    public long getCommentCount() {
        return commentCount;
    }
    public void setCommentCount(long commentCount) {
        this.commentCount = commentCount;
    }
    public void addCommentCount(long number) {
        this.commentCount += number;
    }

    public long getViewCount() {
        return viewCount;
    }
    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }
    public void addViewCount(long number) {
        this.viewCount += number;
    }

    public LocalDateTime getLastViewed() {
        return lastViewed;
    }
    public void setLastViewed(LocalDateTime lastViewed) {
        this.lastViewed = lastViewed;
    }

    public long getThumbnailCount() {
        return thumbnailCount;
    }
    public void setThumbnailCount(long thumbnailCount) {
        this.thumbnailCount = thumbnailCount;
    }
    public void addThumbnailCount(long number) {
        this.thumbnailCount += number;
    }

    public long getAttachmentCount() {
        return attachmentCount;
    }
    public void setAttachmentCount(long attachmentCount) {
        this.attachmentCount = attachmentCount;
    }
    public void addAttachmentCount(long number) {
        this.attachmentCount += number;
    }

    public Map<String, Integer> getParticipants() {
        return this.participants;
    }
    public void setParticipants(Map<String, Integer> participants) {
        this.participants = participants;
    }
    public void addParticipant(String username) {
        if (this.participants == null) {
            this.participants = Map.of(username, 1);
        }
        else {
            this.participants.merge(username, 1, Integer::sum);
        }
    }
}

