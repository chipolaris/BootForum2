package com.github.chipolaris.bootforum2.listener;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.domain.*;
import com.github.chipolaris.bootforum2.event.CommentCreatedEvent;
import com.github.chipolaris.bootforum2.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Component to listen to Comment events
 */
@Component
public class CommentEventsListener {

    private static final Logger logger = LoggerFactory.getLogger(CommentEventsListener.class);
    private static final int MAX_RETRIES = 4; // Total attempts = 1 + MAX_RETRIES
    private static final long INITIAL_BACKOFF_MS = 50;
    private static final long JITTER_MS = 50;

    private final GenericDAO genericDAO;
    private final UserRepository userRepository;

    // Self-injection to allow calling a @Transactional method from a non-transactional one within the same class
    private CommentEventsListener self;

    @Autowired
    @Lazy // @Lazy to break the circular dependency at startup
    public void setSelf(CommentEventsListener self) {
        this.self = self;
    }

    public CommentEventsListener(GenericDAO genericDAO, UserRepository userRepository) {
        this.genericDAO = genericDAO;
        this.userRepository = userRepository;
    }

    /**
     * Main event handler. This method is non-transactional and contains the retry loop.
     * It delegates the actual work to a separate transactional method.
     */
    @TransactionalEventListener
    @Async
    public void handleCommentCreated(CommentCreatedEvent event) {

        final long commentId = event.getComment().getId();

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Delegate to the transactional method
                self.updateStatisticsWithOptimisticLocking(event.getComment());

                // If successful, we're done.
                if (attempt > 0) {
                    logger.info("Successfully updated statistics for comment ID {} on attempt #{}", commentId, attempt + 1);
                }
                return;

            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt < MAX_RETRIES) {
                    // FIX: Use exponential backoff with jitter
                    long backoff = getExponentialBackoff(attempt);
                    logger.warn("Optimistic lock failure for comment ID {} on attempt #{}. Retrying in {}ms...",
                            commentId, attempt + 1, backoff);
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    logger.error("Failed to update statistics for comment ID {} after {} attempts. Giving up.",
                            commentId, MAX_RETRIES + 1, e);
                }
            } catch (Exception e) {
                logger.error("An unexpected error occurred while handling CommentCreatedEvent for comment.id {}. Aborting retries.",
                        commentId, e);
                return; // Abort on other exceptions
            }
        }
    }

    /**
     * Calculates an exponential backoff delay with jitter.
     * @param attempt The current attempt number (0-based).
     * @return The calculated delay in milliseconds.
     */
    private long getExponentialBackoff(int attempt) {
        long delay = (long) (INITIAL_BACKOFF_MS * Math.pow(2, attempt));
        return delay + ThreadLocalRandom.current().nextLong(JITTER_MS);
    }

    /**
     * This method contains the core logic and is executed in its own new transaction.
     * It will throw an ObjectOptimisticLockingFailureException if a concurrent update is detected on commit.
     * @param eventComment The comment from the event.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatisticsWithOptimisticLocking(Comment eventComment) {

        // Re-fetch the comment to ensure it's managed in this new transaction
        Comment comment = genericDAO.find(Comment.class, eventComment.getId());
        if (comment == null) {
            logger.warn("Comment with ID {} was deleted before statistics could be updated.", eventComment.getId());
            return;
        }

        Discussion discussion = comment.getDiscussion();

        // With @Version, we no longer need PESSIMISTIC_WRITE.
        DiscussionStat discussionStat = genericDAO.find(DiscussionStat.class, discussion.getStat().getId());
        ForumStat forumStat = genericDAO.find(ForumStat.class, discussion.getForum().getStat().getId());
        Optional<User> userOpt = userRepository.findByUsername(comment.getCreateBy());

        if (discussionStat != null) {
            updateDiscussionStat(discussionStat, comment);
        }

        if (forumStat != null) {
            updateForumStat(forumStat, comment);
        }

        if (userOpt.isPresent()) {
            UserStat userStat = genericDAO.find(UserStat.class, userOpt.get().getStat().getId());
            if (userStat != null) {
                updateUserStat(userStat, comment);
            }
        }
        // The transaction commits here. If a version mismatch is found, Spring will throw ObjectOptimisticLockingFailureException.
    }

    private void updateUserStat(UserStat userStat, Comment comment) {
        userStat.addCommentCount(1);
        userStat.addImageCount(comment.getImages() != null ? comment.getImages().size() : 0);
        userStat.addAttachmentCount(comment.getAttachments() != null ? comment.getAttachments().size() : 0);

        CommentInfo lastComment = userStat.getLastComment();
        if (lastComment.getCommentDate() == null || lastComment.getCommentDate().isBefore(comment.getCreateDate())) {
            updateCommentInfo(lastComment, comment);
        }
    }

    private void updateForumStat(ForumStat forumStat, Comment comment) {
        forumStat.addCommentCount(1);

        CommentInfo lastComment = forumStat.getLastComment();
        if (lastComment.getCommentDate() == null || lastComment.getCommentDate().isBefore(comment.getCreateDate())) {
            updateCommentInfo(lastComment, comment);
        }
    }

    private void updateDiscussionStat(DiscussionStat discussionStat, Comment comment) {
        discussionStat.addCommentCount(1);
        discussionStat.addAttachmentCount(comment.getAttachments() != null ? comment.getAttachments().size() : 0);
        discussionStat.addImageCount(comment.getImages() != null ? comment.getImages().size() : 0);
        discussionStat.addParticipant(comment.getCreateBy());

        CommentInfo lastComment = discussionStat.getLastComment();

        if (lastComment.getCommentDate() == null || lastComment.getCommentDate().isBefore(comment.getCreateDate())) {
            updateCommentInfo(lastComment, comment);
        }
    }

    private void updateCommentInfo(CommentInfo commentInfo, Comment comment) {
        commentInfo.setCommentId(comment.getId());
        commentInfo.setCommentor(comment.getCreateBy());
        commentInfo.setCommentDate(comment.getCreateDate());
        commentInfo.setTitle(comment.getTitle());
        commentInfo.setContentAbbr(comment.getContent());
    }
}