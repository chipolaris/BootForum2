package com.github.chipolaris.bootforum2.domain;

import java.util.Calendar;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Basic;
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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

@Entity
@Table(name="COMMENT_T")
@TableGenerator(name="CommentIdGenerator", table="ENTITY_ID_T", pkColumnName="GEN_KEY",
        pkColumnValue="COMMENT_ID", valueColumnName="GEN_VALUE", initialValue = 1000, allocationSize=10)
public class Comment extends BaseEntity {

    @PrePersist
    public void prePersist() {
        this.setCreateDate(LocalDateTime.now());
    }

    @PreUpdate
    public void preUpdate() {
        this.setUpdateDate(LocalDateTime.now());
    }

    // persisted attributes
    @Id
    @GeneratedValue(strategy=GenerationType.TABLE, generator="CommentIdGenerator")
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="DISCUSSION_ID", foreignKey = @ForeignKey(name="FK_COMMEN_DISCUS"))
    private Discussion discussion;

    @Column(name="TITLE", length=255)
    private String title;

    @Lob @Basic(fetch=FetchType.LAZY)
    @Column(name="CONTENT")
    private String content; // content of the comment

    @ManyToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="REPLY_TO_ID", foreignKey = @ForeignKey(name="FK_COMMEN_REPLY_TO"))
    private Comment replyTo; // parent of this comment, top level ones will have this field as null

    @OneToMany(fetch=FetchType.LAZY, mappedBy="replyTo")
    @OrderBy("id ASC")
    private List<Comment> replies; // children of this comment

    @Column(name="IP_ADDRESS", length=80)
    private String ipAddress;

    /**
     * OK to eager fetch attachments as only a handful attachments are expected for each comment
     */
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinTable(name="COMMENT_ATTACHMENT_T",
            joinColumns={@JoinColumn(name="COMMENT_ID", foreignKey = @ForeignKey(name="FK_COMMEN_ATTACH_COMMEN"))},
            inverseJoinColumns={@JoinColumn(name="FILE_INFO_ID", foreignKey = @ForeignKey(name="FK_COMMEN_ATTACH_FILE_INFO"))},
            indexes = {@Index(name="IDX_COMMEN_ATTACH", columnList = "COMMENT_ID,FILE_INFO_ID")})
    @OrderColumn(name="SORT_ORDER")
    private List<FileInfo> attachments;

    /**
     * OK to eager fetch attachments as only a handful thumbnails are expected for each comment
     */
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinTable(name="COMMENT_THUMBNAIL_T",
            joinColumns={@JoinColumn(name="COMMENT_ID", foreignKey = @ForeignKey(name="FK_COMMEN_THUMB_COMMEN"))},
            inverseJoinColumns={@JoinColumn(name="FILE_INFO_ID", foreignKey = @ForeignKey(name="FK_COMMEN_ATTACH_FILE_INFO"))},
            indexes = {@Index(name="IDX_COMMEN_THUMBN", columnList = "COMMENT_ID,FILE_INFO_ID")})
    @OrderColumn(name="SORT_ORDER")
    private List<FileInfo> thumbnails;

    @Column(name="HIDDEN")
    private boolean hidden;

    @OneToOne(cascade=CascadeType.ALL)
    @JoinColumn(name="COMMENT_VOTE_ID", foreignKey = @ForeignKey(name="FK_COMMEN_COMMEN_VOTE"))
    private CommentVote commentVote;

    @Override
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public Discussion getDiscussion() {
        return discussion;
    }
    public void setDiscussion(Discussion discussion) {
        this.discussion = discussion;
    }

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public Comment getReplyTo() {
        return replyTo;
    }
    public void setReplyTo(Comment replyTo) {
        this.replyTo = replyTo;
    }

    public List<Comment> getReplies() {
        return replies;
    }
    public void setReplies(List<Comment> replies) {
        this.replies = replies;
    }

    public String getIpAddress() {
        return ipAddress;
    }
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public List<FileInfo> getAttachments() {
        return attachments;
    }
    public void setAttachments(List<FileInfo> attachments) {
        this.attachments = attachments;
    }

    public List<FileInfo> getThumbnails() {
        return thumbnails;
    }
    public void setThumbnails(List<FileInfo> thumbnails) {
        this.thumbnails = thumbnails;
    }

    public boolean getHidden() {
        return hidden;
    }
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public CommentVote getCommentVote() {
        return commentVote;
    }
    public void setCommentVote(CommentVote commentVote) {
        this.commentVote = commentVote;
    }
}