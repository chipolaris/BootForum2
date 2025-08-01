package com.github.chipolaris.bootforum2.listener;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.domain.*;
import com.github.chipolaris.bootforum2.event.DiscussionCreatedEvent;
import com.github.chipolaris.bootforum2.event.DiscussionViewedEvent;
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
 * Component to listen to Discussion events
 */
@Component
public class DiscussionEventsListener {

    private static final Logger logger = LoggerFactory.getLogger(DiscussionEventsListener.class);
    private static final int MAX_RETRIES = 4; // Total attempts = 1 + MAX_RETRIES
    private static final long INITIAL_BACKOFF_MS = 50;
    private static final long JITTER_MS = 50;

    private final GenericDAO genericDAO;
    private final UserRepository userRepository;

    // Self-injection to allow calling a @Transactional method from a non-transactional one
    private DiscussionEventsListener self;

    @Autowired
    @Lazy // @Lazy to break the circular dependency at startup
    public void setSelf(DiscussionEventsListener self) {
        this.self = self;
    }

    public DiscussionEventsListener(GenericDAO genericDAO, UserRepository userRepository) {
        this.genericDAO = genericDAO;
        this.userRepository = userRepository;
    }

    /**
     * DiscussionCreatedEvent listener. This is the non-transactional entry point with the retry loop.
     * @param event The discussion creation event.
     */
    @TransactionalEventListener
    @Async
    public void handleDiscussionCreated(DiscussionCreatedEvent event) {
        final long discussionId = event.getDiscussion().getId();

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Delegate to the transactional method
                self.updateCreationStatisticsWithOptimisticLocking(event.getDiscussion());

                if (attempt > 0) {
                    logger.info("Successfully updated statistics for discussion ID {} on attempt #{}", discussionId, attempt + 1);
                }
                return; // Success, exit the loop

            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt < MAX_RETRIES) {
                    // FIX: Use exponential backoff with jitter
                    long backoff = getExponentialBackoff(attempt);
                    logger.warn("Optimistic lock failure for discussion ID {} on attempt #{}. Retrying in {}ms...",
                            discussionId, attempt + 1, backoff);
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    logger.error("Failed to update statistics for discussion ID {} after {} attempts. Giving up.",
                            discussionId, MAX_RETRIES + 1, e);
                }
            } catch (Exception e) {
                logger.error("An unexpected error occurred while handling DiscussionCreatedEvent for discussion.id {}. Aborting retries.",
                        discussionId, e);
                return; // Abort on other exceptions
            }
        }
    }

    /**
     * This method contains the core logic for creation stats and is executed in its own new transaction.
     * It will throw an ObjectOptimisticLockingFailureException if a concurrent update is detected on commit.
     * @param eventDiscussion The discussion from the event.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateCreationStatisticsWithOptimisticLocking(Discussion eventDiscussion) {
        // Re-fetch to ensure the entity is managed in this transaction
        Discussion discussion = genericDAO.find(Discussion.class, eventDiscussion.getId());
        if (discussion == null) {
            logger.warn("Discussion with ID {} was deleted before statistics could be updated.", eventDiscussion.getId());
            return;
        }

        String username = discussion.getCreateBy();

        // With @Version, we no longer need PESSIMISTIC_WRITE.
        ForumStat forumStat = genericDAO.find(ForumStat.class, discussion.getForum().getStat().getId());
        Optional<User> userOpt = userRepository.findByUsername(username);

        // DiscussionStat is unique to this discussion, so no lock is needed for its own updates.
        updateDiscussionStat(discussion.getStat(), discussion);

        if (forumStat != null) {
            updateForumStat(forumStat, discussion);
        }

        if (userOpt.isPresent()) {
            UserStat userStat = genericDAO.find(UserStat.class, userOpt.get().getStat().getId());
            if (userStat != null) {
                updateUserStat(userStat, discussion);
            }
        }
        // The transaction commits here. If a version mismatch is found, Spring will throw ObjectOptimisticLockingFailureException.
    }

    private void updateDiscussionStat(DiscussionStat discussionStat, Discussion discussion) {
        discussionStat.addParticipant(discussion.getCreateBy());
        discussionStat.setImageCount(discussion.getImages() != null ? discussion.getImages().size() : 0);
        discussionStat.setAttachmentCount(discussion.getAttachments() != null ? discussion.getAttachments().size() : 0);
    }

    private void updateUserStat(UserStat userStat, Discussion discussion) {
        userStat.addDiscussionCount(1);
        userStat.addImageCount(discussion.getImages() != null ? discussion.getImages().size() : 0);
        userStat.addAttachmentCount(discussion.getAttachments() != null ? discussion.getAttachments().size() : 0);

        DiscussionInfo lastDiscussion = userStat.getLastDiscussion();
        if (lastDiscussion.getDiscussionCreateDate() == null ||
                lastDiscussion.getDiscussionCreateDate().isBefore(discussion.getCreateDate())) {
            updateDiscussionInfo(lastDiscussion, discussion);
        }
    }

    private void updateForumStat(ForumStat forumStat, Discussion discussion) {
        forumStat.addDiscussionCount(1);

        DiscussionInfo lastDiscussion = forumStat.getLastDiscussion();
        if (lastDiscussion.getDiscussionCreateDate() == null ||
                lastDiscussion.getDiscussionCreateDate().isBefore(discussion.getCreateDate())) {
            updateDiscussionInfo(lastDiscussion, discussion);
        }
    }

    /**
     * Helper method to populate a DiscussionInfo object from a Discussion.
     */
    private void updateDiscussionInfo(DiscussionInfo discussionInfo, Discussion discussion) {
        discussionInfo.setDiscussionId(discussion.getId());
        discussionInfo.setDiscussionCreateDate(discussion.getCreateDate());
        discussionInfo.setDiscussionCreator(discussion.getCreateBy());
        discussionInfo.setTitle(discussion.getTitle());
        discussionInfo.setContentAbbr(discussion.getContent());
    }

    /**
     * DiscussionViewedEvent listener. This is the non-transactional entry point with the retry loop.
     * @param event The discussion view event.
     */
    @TransactionalEventListener
    @Async
    public void handleDiscussionViewed(DiscussionViewedEvent event) {
        final long discussionId = event.getDiscussion().getId();

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                self.incrementViewCountWithOptimisticLocking(discussionId);
                return; // Success
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt < MAX_RETRIES) {
                    // FIX: Use exponential backoff with jitter
                    long backoff = getExponentialBackoff(attempt);
                    logger.warn("Optimistic lock failure on view count for discussion ID {} on attempt #{}. Retrying in {}ms...",
                            discussionId, attempt + 1, backoff);
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    logger.error("Failed to increment view count for discussion ID {} after {} attempts. Giving up.",
                            discussionId, MAX_RETRIES + 1, e);
                }
            } catch (Exception e) {
                logger.error("An unexpected error occurred while incrementing view count for discussion.id {}.",
                        discussionId, e);
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
     * This method contains the core logic for view count and is executed in its own new transaction.
     * @param discussionId The ID of the discussion to update.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementViewCountWithOptimisticLocking(Long discussionId) {
        Discussion discussion = genericDAO.find(Discussion.class, discussionId);
        if (discussion != null && discussion.getStat() != null) {
            discussion.getStat().addViewCount(1);
        } else {
            logger.warn("Could not increment view count. Discussion or its stats not found for ID: {}", discussionId);
        }
    }
}