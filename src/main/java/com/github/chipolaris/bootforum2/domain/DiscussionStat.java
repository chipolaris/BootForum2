package com.github.chipolaris.bootforum2.domain;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.*;

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

    @Column(name="VOTE_UP_COUNT")
    private int voteUpCount;

    @Column(name="VOTE_DOWN_COUNT")
    private int voteDownCount;

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY)
    @JoinTable(name="DISCUSSION_STAT_VOTE_T",
            joinColumns={@JoinColumn(name="DISCUSSION_STAT_ID", foreignKey = @ForeignKey(name="FK_DISCUS_STAT_VOT_DISCUS_STAT"))},
            inverseJoinColumns={@JoinColumn(name="VOTE_ID", foreignKey = @ForeignKey(name="FK_DISCUS_STAT_VOT_VOTE"))},
            indexes = {@Index(name="IDX_DISCUSSION_STAT_VOTE", columnList = "DISCUSSION_STAT_ID,VOTE_ID")})
    private Set<Vote> votes;

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

    public int getVoteUpCount() {
        return voteUpCount;
    }
    public void setVoteUpCount(int voteUpCount) {
        this.voteUpCount = voteUpCount;
    }
    public void addVoteUpCount() { this.voteUpCount++; }

    public int getVoteDownCount() {
        return voteDownCount;
    }
    public void setVoteDownCount(int voteDownCount) {
        this.voteDownCount = voteDownCount;
    }
    public void addVoteDownCount() { this.voteDownCount++; }

    public Set<Vote> getVotes() {
        return votes;
    }
    public void setVotes(Set<Vote> votes) {
        this.votes = votes;
    }
}

