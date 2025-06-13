package com.github.chipolaris.bootforum2.listener;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.domain.Comment;
import com.github.chipolaris.bootforum2.domain.CommentInfo;
import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.domain.DiscussionStat;
import com.github.chipolaris.bootforum2.event.CommentCreatedEvent;
import com.github.chipolaris.bootforum2.event.DiscussionViewedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.common.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

@Deprecated
//@Component
public class DiscussionStatUpdateListener { // No longer implements ApplicationListener

    private static final Logger logger = LoggerFactory.getLogger(DiscussionStatUpdateListener.class);

    private final GenericDAO genericDAO;

    public DiscussionStatUpdateListener(GenericDAO genericDAO) {
        this.genericDAO = genericDAO;
    }

    @EventListener // Use @EventListener for specific event types
    @Transactional // This listener method should run in its own transaction
    @Async         // Make this listener asynchronous
    public void handleDiscussionViewedEvent(DiscussionViewedEvent event) { // Method name can be more specific
        Discussion discussion = event.getDiscussion();
        if (discussion == null || discussion.getStat() == null) {
            logger.warn("Discussion or its statistics are null in DiscussionViewedEvent. Cannot update view count.");
            return;
        }

        // It's often a good practice to re-fetch the entity in an async, transactional listener
        // to ensure you're working with the latest state and to avoid potential detached entity issues.
        // However, if DiscussionStat is always updated via its owning Discussion,
        // and the event carries a managed Discussion, this might be okay.
        // For direct updates to DiscussionStat, fetching it is safer.
        DiscussionStat discussionStat = genericDAO.find(DiscussionStat.class, discussion.getStat().getId());

        if (discussionStat == null) {
            logger.warn("DiscussionStat not found for ID: {}. Cannot update view count.", discussion.getStat().getId());
            return;
        }

        // Increment view count
        discussionStat.addViewCount(1);

        // Update last viewed time
        discussionStat.setLastViewed(LocalDateTime.now());

        try {
            // Persist the changes to DiscussionStat
            genericDAO.merge(discussionStat);
            logger.info("Updated view count and last viewed time for discussion ID: {}", discussion.getId());
        } catch (Exception e) {
            logger.error("Failed to update discussion statistics for discussion ID: " + discussion.getId(), e);
            // Consider how to handle this error, e.g., retry, log for manual intervention.
            // For now, we'll just log it. The main discussion view operation will still succeed.
        }
    }

    @EventListener // Use @EventListener for specific event types
    @Transactional // This listener method should run in its own transaction
    @Async         // Make this listener asynchronous
    public void handleCommentCreatedEvent(CommentCreatedEvent event) {

        Comment comment = event.getComment();

        if (comment == null) {
            logger.warn("Discussion or its statistics are null in DiscussionViewedEvent. Cannot update view count.");
            return;
        }

        Discussion discussion = comment.getDiscussion();
        if (discussion == null) {
            logger.warn("Discussion is null");
            return;
        }
        DiscussionStat discussionStat = discussion.getStat();
        if (discussionStat == null) {
            logger.warn("DiscussionStat is null");
            return;
        }

        discussionStat.addCommentCount(1);
        discussionStat.addAttachmentCount(comment.getAttachments().size());
        discussionStat.addThumbnailCount(comment.getThumbnails().size());
        Map<String, Integer> commentors = discussionStat.getParticipants();
        commentors.merge(event.getUsername(), 1, Integer::sum);

        CommentInfo commentInfo = discussionStat.getLastComment();
        if (commentInfo.getCommentDate().isBefore(comment.getCreateDate())) {
            commentInfo.setCommentDate(comment.getCreateDate());
            commentInfo.setTitle(comment.getTitle());
            commentInfo.setContentAbbr(StringUtils.truncate(comment.getContent(), 255));
            commentInfo.setCommentor(comment.getCreateBy());
            commentInfo.setCommentId(comment.getId());
        }

        // Persist the changes to DiscussionStat
        genericDAO.merge(discussionStat);

        logger.info("Updated comment count for discussion ID: {}", discussion.getId());
    }
}