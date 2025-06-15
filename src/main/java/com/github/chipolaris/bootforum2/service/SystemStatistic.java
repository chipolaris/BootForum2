package com.github.chipolaris.bootforum2.service; // Or a more suitable package

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.domain.*; // Assuming your domain entities are here
import com.github.chipolaris.bootforum2.dto.CommentInfoDTO;
import com.github.chipolaris.bootforum2.dto.DiscussionInfoDTO;
import com.github.chipolaris.bootforum2.dto.SystemStatisticDTO;
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
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SystemStatistic {

    private static final Logger logger = LoggerFactory.getLogger(SystemStatistic.class);

    // --- Data Attributes ---
    private volatile CommentInfoDTO lastComment;
    private DiscussionInfoDTO lastDiscussion;
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
            String truncatedContent = StringUtils.truncate(latestDbComment.getContent(), 255);
            this.lastComment = new CommentInfoDTO(latestDbComment.getId(), latestDbComment.getTitle(), truncatedContent,
                    latestDbComment.getCreateBy(), latestDbComment.getCreateDate()); // Ensure lastComment is initialized
        }

        // Load last discussion
        Discussion latestDbDiscussion = genericDAO.greatest(Discussion.class, "createDate");
        if(latestDbDiscussion != null) {
            String truncatedContent = StringUtils.truncate(latestDbDiscussion.getContent(), 255);
            this.lastDiscussion = new DiscussionInfoDTO(latestDbDiscussion.getId(), latestDbDiscussion.getTitle(),
                    truncatedContent, latestDbDiscussion.getCreateBy(), latestDbDiscussion.getCreateDate());
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

    public SystemStatisticDTO getDTO() {
        return new SystemStatisticDTO(this.userCount.get(), this.forumCount.get(), this.discussionCount.get(),
                this.commentCount.get(), this.lastRegisteredUser, this.lastUserRegisteredDate,
                this.lastComment, this.lastDiscussion);
    }

    // --- Getters (Thread-safe for reads) ---
    public CommentInfoDTO getLastComment() { return lastComment; }

    public DiscussionInfoDTO getLastDiscussion() { return lastDiscussion; }

    public long getCommentCount() { return commentCount.get(); }

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

    public synchronized void updateLastComment(Comment newComment) {
        if (newComment == null || newComment.getCreateDate() == null) {
            return;
        }
        if (this.lastComment == null || this.lastComment.commentDate() == null ||
                newComment.getCreateDate().isAfter(this.lastComment.commentDate())) {
            String truncatedContent = StringUtils.truncate(newComment.getContent(), 255);
            this.lastComment = new CommentInfoDTO(newComment.getId(), newComment.getTitle(),
                    truncatedContent, newComment.getCreateBy(), newComment.getCreateDate()); // Assuming newComment is a complete, new object
            logger.debug("Updated last comment to: {}", newComment.getId());
        }
    }

    public synchronized void updateLastDiscussion(Discussion newDiscussion) {
        if (newDiscussion == null || newDiscussion.getCreateDate() == null) {
            return;
        }
        if (this.lastDiscussion == null || this.lastDiscussion.discussionCreateDate() == null ||
                newDiscussion.getCreateDate().isAfter(this.lastDiscussion.discussionCreateDate())) {
            String truncatedContent = StringUtils.truncate(newDiscussion.getContent(), 255);
            this.lastDiscussion = new DiscussionInfoDTO(newDiscussion.getId(), newDiscussion.getTitle(),
                    truncatedContent, newDiscussion.getCreateBy(), newDiscussion.getCreateDate()); // Assuming newComment is a complete, new object
            logger.debug("Updated last discussion to: {}", newDiscussion.getId());
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
            sb.append(String.format("\n  ID: %s", lastComment.commentId() != null ? lastComment.commentId() : "N/A"));
            sb.append(String.format("\n  Title: %s", lastComment.title() != null ? lastComment.title() : "N/A"));
            sb.append(String.format("\n  Commentor: %s", lastComment.commentor() != null ? lastComment.commentor() : "N/A"));
            sb.append(String.format("\n  Date: %s",
                    lastComment.commentDate() != null ? lastComment.commentDate().format(formatter) : "N/A"));
            sb.append(String.format("\n  Content Abbr: %s", lastComment.contentAbbr() != null ? lastComment.contentAbbr() : "N/A"));
        } else {
            sb.append("\nLast Comment Details: N/A");
        }

        if (lastDiscussion != null) {
            sb.append("\nLast Discussion Details:");
            sb.append(String.format("\n  ID: %s", lastDiscussion.discussionId() != null ? lastDiscussion.discussionId() : "N/A"));
            sb.append(String.format("\n  Title: %s", lastDiscussion.title() != null ? lastDiscussion.title() : "N/A"));
            sb.append(String.format("\n  CreateBy: %s", lastDiscussion.discussionCreator() != null ? lastDiscussion.discussionCreator() : "N/A"));
            sb.append(String.format("\n  Date: %s",
                    lastDiscussion.discussionCreateDate() != null ? lastDiscussion.discussionCreateDate().format(formatter) : "N/A"));
            sb.append(String.format("\n  Content Abbr: %s", lastDiscussion.contentAbbr() != null ? lastDiscussion.contentAbbr() : "N/A"));
        } else {
            sb.append("\nLast Discussion Details: N/A");
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

        this.incrementCommentCount();

        this.updateLastComment(comment);

        logger.info("Handling CommentCreatedEvent for Comment: {}", comment.getTitle());
    }

    @EventListener
    @Async // Uncomment for asynchronous execution (requires @EnableAsync in a config class)
    public void handleDiscussionCreatedEvent(DiscussionCreatedEvent event) {

        /*
         * When a discussion is created:
         *  - increase discussion count
         *  - update last discussion info
         */

        Discussion discussion = event.getDiscussion();
        incrementDiscussionCount();
        updateLastDiscussion(event.getDiscussion());

        logger.info("Handling DiscussionCreatedEvent created {}. Discussion count: {}",
                discussion.getTitle(), discussionCount.get());
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

        this.incrementUserCount();
        this.updateUserRegistration(user.getUsername(), user.getCreateDate());

        logger.info("Handling UserCreatedEvent for User: {}", user.getUsername());
    }
}