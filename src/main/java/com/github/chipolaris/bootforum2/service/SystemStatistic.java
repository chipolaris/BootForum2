package com.github.chipolaris.bootforum2.service; // Or a more suitable package

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.*; // Assuming your domain entities are here
import com.github.chipolaris.bootforum2.event.*;
import com.github.chipolaris.bootforum2.mapper.CommentInfoMapper;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SystemStatistic {

    private static final Logger logger = LoggerFactory.getLogger(SystemStatistic.class);

    // --- Data Attributes ---
    private volatile CommentInfo lastComment;
    private final AtomicLong commentCount = new AtomicLong(0);
    private final AtomicLong discussionCount = new AtomicLong(0);
    private final AtomicLong forumCount = new AtomicLong(0);
    private final AtomicLong userCount = new AtomicLong(0);
    private final AtomicLong forumGroupCount = new AtomicLong(0); // Assuming ForumGroup entity exists
    private final AtomicLong chatRoomCount = new AtomicLong(0);   // Assuming ChatRoom entity exists

    private volatile String lastRegisteredUser;
    private volatile LocalDateTime lastUserRegisteredDate;

    @Autowired
    private GenericDAO genericDAO; // Used for initial loading

    @Autowired
    private DynamicDAO dynamicDAO;

    @Autowired
    private CommentInfoMapper commentInfoMapper; // For mapping Comment to CommentInfo

    // --- Initialization ---
    @PostConstruct
    public void initializeStatistics() {
        logger.info("Initializing System Statistics...");

        // Load initial counts
        this.commentCount.set(genericDAO.count(Comment.class));
        this.discussionCount.set(genericDAO.count(Discussion.class));
        this.forumCount.set(genericDAO.count(Forum.class));
        this.forumGroupCount.set(genericDAO.count(ForumGroup.class));
        this.userCount.set(genericDAO.count(User.class));
        // Add counts for ForumGroup and ChatRoom if entities exist
        // Example: this.forumGroupCount.set(genericDAO.count(ForumGroup.class));
        // Example: this.chatRoomCount.set(genericDAO.count(ChatRoom.class));

        // Load last comment
        Comment latestDbComment = genericDAO.greatest(Comment.class, "createDate");
        if (latestDbComment != null) {
            // It's safer to create a new CommentInfo object rather than trying to set fields on a potentially null one.
            this.lastComment = new CommentInfo(); // Ensure lastComment is initialized
            this.lastComment.setCommentId(latestDbComment.getId());
            this.lastComment.setCommentDate(latestDbComment.getCreateDate());
            this.lastComment.setCommentor(latestDbComment.getCreateBy());
            this.lastComment.setTitle(latestDbComment.getTitle());
            // The StringUtils.truncate method from micrometer returns the truncated string,
            // it does not modify the original string in place.
            String truncatedContent = StringUtils.truncate(latestDbComment.getContent(), 255);
            this.lastComment.setContentAbbr(truncatedContent);
        }


        // Load last registered user
        User latestUser = genericDAO.greatest(User.class, "createDate");

        if(latestUser != null) {
            this.lastRegisteredUser = latestUser.getUsername();
            this.lastUserRegisteredDate = latestUser.getCreateDate();
        }

        logger.info("System Statistics Initialized: Users={}, Discussions={}, Comments={}",
                this.userCount.get(), this.discussionCount.get(), this.commentCount.get());

        printStatistics(); // Optionally print stats after initialization
    }

    // --- Getters (Thread-safe for reads) ---
    public CommentInfo getLastComment() {
        return lastComment; // CommentInfo should be immutable or a defensive copy returned if mutable
    }

    public long getCommentCount() {
        return commentCount.get();
    }

    public long getDiscussionCount() {
        return discussionCount.get();
    }

    public long getForumCount() {
        return forumCount.get();
    }

    public long getUserCount() {
        return userCount.get();
    }

    public long getForumGroupCount() {
        return forumGroupCount.get();
    }

    public long getChatRoomCount() {
        return chatRoomCount.get();
    }

    public String getLastRegisteredUser() {
        return lastRegisteredUser;
    }

    public LocalDateTime getLastUserRegisteredDate() {
        // LocalDateTime is immutable, so no need to return a copy
        return lastUserRegisteredDate;
    }

    // --- Updaters (Synchronized for thread-safety on compound objects) ---

    public synchronized void updateLastComment(CommentInfo newCommentInfo) {
        if (newCommentInfo == null || newCommentInfo.getCommentDate() == null) {
            return;
        }
        if (this.lastComment == null || this.lastComment.getCommentDate() == null ||
                newCommentInfo.getCommentDate().isAfter(this.lastComment.getCommentDate())) {
            this.lastComment = newCommentInfo; // Assuming newCommentInfo is a complete, new object
            logger.debug("Updated last comment to: {}", newCommentInfo.getCommentId());
        }
    }

    public void incrementCommentCount() {
        this.commentCount.incrementAndGet();
    }

    public void incrementDiscussionCount() {
        this.discussionCount.incrementAndGet();
    }

    public void incrementForumCount() {
        this.forumCount.incrementAndGet();
    }

    public void incrementUserCount() {
        this.userCount.incrementAndGet();
    }

    public void incrementForumGroupCount() {
        this.forumGroupCount.incrementAndGet();
    }

    public void incrementChatRoomCount() {
        this.chatRoomCount.incrementAndGet();
    }

    public synchronized void updateUserRegistration(String username, LocalDateTime registrationDate) {
        if (username == null || registrationDate == null) {
            return;
        }
        if (this.lastUserRegisteredDate == null || registrationDate.isAfter(this.lastUserRegisteredDate)) {
            this.lastRegisteredUser = username;
            this.lastUserRegisteredDate = registrationDate; // LocalDateTime is immutable
            logger.debug("Updated last registered user to: {}", username);
        }
    }

    /**
     * Utility method to print all current system statistics to the log.
     */
    public void printStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- System Statistics ---");
        sb.append(String.format("\nComment Count: %d", commentCount.get()));
        sb.append(String.format("\nDiscussion Count: %d", discussionCount.get()));
        sb.append(String.format("\nForum Count: %d", forumCount.get()));
        sb.append(String.format("\nUser Count: %d", userCount.get()));
        sb.append(String.format("\nForum Group Count: %d", forumGroupCount.get()));
        sb.append(String.format("\nChat Room Count: %d", chatRoomCount.get()));
        sb.append(String.format("\nLast Registered User: %s", lastRegisteredUser != null ? lastRegisteredUser : "N/A"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        sb.append(String.format("\nLast User Registered Date: %s",
                lastUserRegisteredDate != null ? lastUserRegisteredDate.format(formatter) : "N/A"));

        if (lastComment != null) {
            sb.append("\nLast Comment Details:");
            sb.append(String.format("\n  ID: %s", lastComment.getCommentId() != null ? lastComment.getCommentId().toString() : "N/A"));
            sb.append(String.format("\n  Title: %s", lastComment.getTitle() != null ? lastComment.getTitle() : "N/A"));
            sb.append(String.format("\n  Commentor: %s", lastComment.getCommentor() != null ? lastComment.getCommentor() : "N/A"));
            sb.append(String.format("\n  Date: %s",
                    lastComment.getCommentDate() != null ? lastComment.getCommentDate().format(formatter) : "N/A"));
            sb.append(String.format("\n  Content Abbr: %s", lastComment.getContentAbbr() != null ? lastComment.getContentAbbr() : "N/A"));
        } else {
            sb.append("\nLast Comment Details: N/A");
        }
        sb.append("\n-------------------------");

        logger.info(sb.toString());
    }

    // ---------- Event listeners

    @EventListener
    @Transactional(readOnly = true) // need this to fetch comment's discussion
    @Async
    public void handleCommentCreatedEvent(CommentCreatedEvent event) {
        Comment comment = event.getComment();
        logger.info("Handling CommentCreatedEvent for Comment: {}", comment.getTitle());

        this.incrementCommentCount();

        CommentInfo commentInfo = comment.getDiscussion().getStat().getLastComment();
        this.updateLastComment(commentInfo);
    }

    @EventListener
    @Async // Uncomment for asynchronous execution (requires @EnableAsync in a config class)
    public void handleDiscussionCreatedEvent(DiscussionCreatedEvent event) {

        /*
         * When a discussion is created:
         *  - increase discussion count
         *  - increase comment count
         *  - update last comment info
         */

        incrementDiscussionCount();
        incrementCommentCount();
        updateLastComment(event.getDiscussion().getStat().getLastComment());

        logger.info("New discussion created. Discussion count: {}, Comment count: {}",
                discussionCount.get(), commentCount.get());
    }

    @EventListener
    @Async
    public void handleForumCreatedEvent(ForumCreatedEvent event) {

        Forum forum = event.getForum();
        logger.info("Handling ForumCreatedEvent for Forum: {}", forum.getTitle());
        this.incrementForumCount();
    }

    @EventListener
    @Async
    public void handleForumGroupCreatedEvent(ForumGroupCreatedEvent event) {

        ForumGroup forumGroup = event.getForumGroup();
        logger.info("Handling ForumGroupCreatedEvent for Forum: {}", forumGroup.getTitle());
        this.incrementForumGroupCount();
    }

    @EventListener
    @Async
    public void handleUserCreatedEvent(UserCreatedEvent event) {

        /*
         * When a user is created:
         *  - increase user count
         *  - update last registered user info
         */
        User user = event.getUser();
        logger.info("Handling UserCreatedEvent for User: {}", user.getUsername());
        this.incrementUserCount();
        this.updateUserRegistration(user.getUsername(), user.getCreateDate());
    }
}