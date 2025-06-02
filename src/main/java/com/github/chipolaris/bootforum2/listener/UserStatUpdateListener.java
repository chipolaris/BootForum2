package com.github.chipolaris.bootforum2.listener;

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.Comment;
import com.github.chipolaris.bootforum2.domain.CommentInfo;
import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.domain.UserStat;
import com.github.chipolaris.bootforum2.event.CommentVotedEvent;
import com.github.chipolaris.bootforum2.event.DiscussionCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
// import org.springframework.scheduling.annotation.Async; // If you want async

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class UserStatUpdateListener {

    private static final Logger logger = LoggerFactory.getLogger(UserStatUpdateListener.class);

    private final DynamicDAO dynamicDAO; // Changed from GenericDAO
    private final GenericDAO genericDAO; // Keep GenericDAO for merge, or inject EntityManager

    public UserStatUpdateListener(DynamicDAO dynamicDAO, GenericDAO genericDAO) { // Updated constructor
        this.dynamicDAO = dynamicDAO;
        this.genericDAO = genericDAO; // Still needed for merge, unless you refactor merge logic
    }

    @EventListener
    @Transactional // Ensure this operation is transactional
    @Async // Uncomment for asynchronous execution (requires @EnableAsync in a config class)
    public void handleDiscussionCreatedEvent(DiscussionCreatedEvent event) {
        Discussion discussion = event.getDiscussion();
        String creatorUsername = event.getCreatorUsername();

        // 1. Find the user who created the discussion using DynamicDAO
        QuerySpec userQuery = QuerySpec.builder(User.class)
                .filter(FilterSpec.eq("username", creatorUsername))
                .build();

        Optional<User> userOptional = dynamicDAO.findOptional(userQuery);

        if (userOptional.isEmpty()) {
            logger.warn("User '{}' not found. Cannot update user stats for discussion ID {}.", creatorUsername, discussion.getId());
            return;
        }
        User user = userOptional.get();

        logger.info("Handling DiscussionCreatedEvent for user: {}, discussion ID: {}", creatorUsername, discussion.getId());

        UserStat userStat = user.getStat();
        if (userStat == null) {
            // This should ideally be handled by the User entity's constructor or @PrePersist
            logger.warn("UserStat was null for User '{}'. Initializing.", creatorUsername);
            userStat = new UserStat();
            user.setStat(userStat);
        }

        // 2. Update UserStat fields
        userStat.addDiscussionCount(1);

        Comment initialComment = discussion.getComments().stream().findFirst().orElse(null);

        if (initialComment != null && initialComment.getId() != null) {
            userStat.addCommentCount(1); // For the initial comment of the discussion

            // Update last comment for the user if this new comment is more recent
            CommentInfo userLastComment = userStat.getLastComment();
            if (userLastComment == null) {
                userLastComment = new CommentInfo(); // Should be handled by @PrePersist in UserStat
                userStat.setLastComment(userLastComment);
            }

            LocalDateTime initialCommentDate = initialComment.getCreateDate() != null ? initialComment.getCreateDate() : LocalDateTime.now();

            if (userLastComment.getCommentDate() == null || initialCommentDate.isAfter(userLastComment.getCommentDate())) {
                userLastComment.setCommentId(initialComment.getId());
                userLastComment.setTitle(discussion.getTitle()); // Or initialComment.getTitle() if it can differ
                userLastComment.setCommentor(creatorUsername);
                userLastComment.setCommentDate(initialCommentDate);
            }

            // Update attachment/thumbnail counts from the initial comment
            if (initialComment.getAttachments() != null) {
                userStat.addCommentAttachmentCount(initialComment.getAttachments().size());
            }
            if (initialComment.getThumbnails() != null) {
                userStat.addCommentThumbnailCount(initialComment.getThumbnails().size());
            }
        } else {
            logger.warn("Initial comment or its ID not found for discussion ID {}. Some user stats might not be fully updated.", discussion.getId());
        }

        // 3. Persist changes
        // Since UserStat is part of the User aggregate and likely cascaded,
        // merging the User entity should persist changes to UserStat.
        // You can continue using GenericDAO for merge, or if DynamicDAO had a merge method, use that.
        genericDAO.merge(user);
        logger.info("User stats updated for user: {}", creatorUsername);
    }

    @EventListener
    @Transactional
    @Async
    public void handleCommentVotedEvent(CommentVotedEvent event) {
        Comment comment = event.getComment();
        short voteValue = event.getVoteValue();
        // String voterUsername = event.getVoterUsername(); // User who cast the vote

        if (comment == null || comment.getCreateBy() == null) {
            logger.warn("Comment or comment creator is null in CommentVotedEvent. Cannot update reputation.");
            return;
        }

        String commentCreatorUsername = comment.getCreateBy();

        // Find the user who created the comment
        QuerySpec userQuery = QuerySpec.builder(User.class)
                .filter(FilterSpec.eq("username", commentCreatorUsername))
                .build();
        Optional<User> userOptional = dynamicDAO.findOptional(userQuery);

        if (userOptional.isEmpty()) {
            logger.warn("User (comment creator) '{}' not found. Cannot update reputation for comment ID {}.",
                    commentCreatorUsername, comment.getId());
            return;
        }

        User commentCreator = userOptional.get();
        UserStat userStat = commentCreator.getStat();

        if (userStat == null) {
            // This should ideally not happen if User entity initializes UserStat
            logger.warn("UserStat was null for comment creator '{}'. Initializing for reputation update.", commentCreatorUsername);
            userStat = new UserStat();
            commentCreator.setStat(userStat);
        }

        // Update reputation
        userStat.addReputation(voteValue);

        try {
            genericDAO.merge(commentCreator); // Persist changes to the User (and cascaded UserStat)
            logger.info("Updated reputation for user '{}' by {} due to vote on comment ID {}. New reputation: {}",
                    commentCreatorUsername, voteValue, comment.getId(), userStat.getReputation());
        } catch (Exception e) {
            logger.error(String.format("Failed to update reputation for user '%s' (comment ID %d)",
                    commentCreatorUsername, comment.getId()), e);
        }
    }
}