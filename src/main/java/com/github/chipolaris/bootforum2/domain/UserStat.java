package com.github.chipolaris.bootforum2.domain;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name="USER_STAT_T")
@TableGenerator(name="UserStatIdGenerator", table="ENTITY_ID_T", pkColumnName="GEN_KEY",
        pkColumnValue="USER_STAT_ID", valueColumnName="GEN_VALUE", initialValue = 1000, allocationSize=10)
public class UserStat extends BaseEntity {
    public UserStat() {}
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.setCreateDate(now);
        this.setUpdateDate(now);
    }

    @Id
    @GeneratedValue(strategy=GenerationType.TABLE, generator="UserStatIdGenerator")
    private Long id;

    @OneToOne(fetch=FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name="LAST_COMMENT_INFO_ID", foreignKey = @ForeignKey(name="FK_USER_STAT_LAST_COMMENT"))
    private CommentInfo lastComment; // info about last comment, used for display

    @OneToOne(fetch=FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name="LAST_DISCUSSION_INFO_ID", foreignKey = @ForeignKey(name="FK_USER_STAT_LAST_DISCUSSION"))
    private DiscussionInfo lastDiscussion; // info about last discussion, used for display

    @Column(name="THUMBNAIL_COUNT")
    private long thumbnailCount;

    @Column(name="ATTACHMENT_COUNT")
    private long attachmentCount;

    @Column(name="COMMENT_COUNT")
    private long commentCount;

    @Column(name="DISCUSSION_COUNT")
    private long discussionCount; // number of discussion started

    @Column(name="REPUTATION")
    private long reputation;

    @Column(name="PROFILE_VIEWED")
    private long profileViewed;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="LAST_LOGIN")
    private LocalDateTime lastLogin;

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

    public DiscussionInfo getLastDiscussion() { return lastDiscussion; }
    public void setLastDiscussion(DiscussionInfo lastDiscussion) {
        this.lastDiscussion = lastDiscussion;
    }

    public long getThumbnailCount() {
        return thumbnailCount;
    }
    public void setThumbnailCount(long thumbnailCount) {
        this.thumbnailCount = thumbnailCount;
    }
    public void addThumbnailCount(long value) {
        this.thumbnailCount += value;
    }

    public long getAttachmentCount() {
        return attachmentCount;
    }
    public void setAttachmentCount(long attachmentCount) { this.attachmentCount = attachmentCount; }
    public void addAttachmentCount(long value) {
        this.attachmentCount += value;
    }

    public long getCommentCount() {
        return commentCount;
    }
    public void setCommentCount(long commentCount) {
        this.commentCount = commentCount;
    }
    public void addCommentCount(long value) {
        this.commentCount += value;
    }

    public long getDiscussionCount() {
        return discussionCount;
    }
    public void setDiscussionCount(long discussionCount) {
        this.discussionCount = discussionCount;
    }
    public void addDiscussionCount(long value) {
        this.discussionCount += value;
    }

    public long getReputation() {
        return reputation;
    }
    public void setReputation(long reputation) {
        this.reputation = reputation;
    }
    public void addReputation(long value) {
        this.reputation += value;
    }

    public long getProfileViewed() {
        return profileViewed;
    }
    public void setProfileViewed(long profileViewed) {
        this.profileViewed = profileViewed;
    }
    public void addProfileViewed(long value) {
        this.profileViewed += value;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
}