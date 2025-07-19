package com.github.chipolaris.bootforum2.domain;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

@Entity
@Table(name="DISCUSSION_T")
@TableGenerator(name="DiscussionIdGenerator", table="ENTITY_ID_T", pkColumnName="GEN_KEY",
        pkColumnValue="DISCUSSION_ID", valueColumnName="GEN_VALUE", initialValue = 1000, allocationSize=10)
@Indexed
public class Discussion extends BaseEntity {

    public static Discussion newDiscussion() {
        Discussion discussion = new Discussion();

        DiscussionStat discussionStat = new DiscussionStat();
        discussionStat.setLastComment(new CommentInfo());
        discussion.setStat(discussionStat);

        return discussion;
    }

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
    @GeneratedValue(strategy=GenerationType.TABLE, generator="DiscussionIdGenerator")
    private Long id;

    @FullTextField
    @Column(name="TITLE", length=255, nullable=false)
    private String title;

    @Column(name="CLOSED")
    private boolean closed;

    @Column(name="STICKY")
    private boolean sticky;

    @Column(name="IMPORTANT")
    private boolean important;

    @FullTextField
    @Lob
    @Basic(fetch=FetchType.LAZY)
    @Column(name="CONTENT")
    private String content; // content of the discussion

    @OneToMany(fetch=FetchType.LAZY, mappedBy="discussion", cascade=CascadeType.ALL)
    @OrderBy("id ASC")
    private List<Comment> comments; // all comments for this discussion thread

    /**
     * OK to eager fetch attachments as only a handful attachments are expected for each comment
     */
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinTable(name="DISCUSSION_ATTACHMENT_T",
            joinColumns={@JoinColumn(name="DISCUSSION_ID", foreignKey = @ForeignKey(name="FK_DISCUS_ATTACH_DISCUS"))},
            inverseJoinColumns={@JoinColumn(name="FILE_INFO_ID", foreignKey = @ForeignKey(name="FK_DISCUS_ATTACH_FILE_INFO"))},
            indexes = {@Index(name="IDX_DISCUS_ATTACH", columnList = "DISCUSSION_ID,FILE_INFO_ID")})
    @OrderColumn(name="SORT_ORDER")
    private List<FileInfo> attachments;

    /**
     * OK to eager fetch attachments as only a handful images are expected for each comment
     */
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinTable(name="DISCUSSION_IMAGE_T",
            joinColumns={@JoinColumn(name="DISCUSSION_ID", foreignKey = @ForeignKey(name="FK_DISCUS_IMG_DISCUS"))},
            inverseJoinColumns={@JoinColumn(name="FILE_INFO_ID", foreignKey = @ForeignKey(name="FK_DISCUS_ATTACH_FILE_INFO"))},
            indexes = {@Index(name="IDX_DISCUS_IMG", columnList = "DISCUSSION_ID,FILE_INFO_ID")})
    @OrderColumn(name="SORT_ORDER")
    private List<FileInfo> images;

    @OneToOne(fetch=FetchType.EAGER, cascade=CascadeType.ALL)
    @JoinColumn(name="DISCUSSION_STAT_ID")
    private DiscussionStat stat;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="FORUM_ID", foreignKey=@ForeignKey(name="FK_DISCUSSION_FORUM"))
    private Forum forum;

    @ManyToMany(fetch=FetchType.EAGER)
    @JoinTable(name="DISCUSSION_TAG_T",
            joinColumns={@JoinColumn(name="DISCUSSION_ID", foreignKey = @ForeignKey(name="FK_DISCUS_TAG_DISCUSSION"))},
            inverseJoinColumns={@JoinColumn(name="TAG_ID", foreignKey = @ForeignKey(name="FK_DISCUS_TAG_TAG"))},
            indexes = {@Index(name="IDX_DISCUS_TAG", columnList = "DISCUSSION_ID,TAG_ID")})
    private List<Tag> tags;

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

    public boolean isClosed() {
        return closed;
    }
    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public boolean isSticky() {
        return sticky;
    }
    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }

    public boolean isImportant() {
        return important;
    }
    public void setImportant(boolean important) {
        this.important = important;
    }

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public List<Comment> getComments() {
        return comments;
    }
    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<FileInfo> getAttachments() {
        return attachments;
    }
    public void setAttachments(List<FileInfo> attachments) {
        this.attachments = attachments;
    }

    public List<FileInfo> getImages() {
        return images;
    }
    public void setImages(List<FileInfo> images) {
        this.images = images;
    }

    public DiscussionStat getStat() {
        return stat;
    }
    public void setStat(DiscussionStat stat) {
        this.stat = stat;
    }

    public Forum getForum() {
        return forum;
    }
    public void setForum(Forum forum) {
        this.forum = forum;
    }

    public List<Tag> getTags() {
        return tags;
    }
    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }
}