package com.github.chipolaris.bootforum2.listener;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.domain.Comment;
import com.github.chipolaris.bootforum2.domain.CommentInfo;
import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.domain.Forum;
import com.github.chipolaris.bootforum2.domain.ForumStat;
import com.github.chipolaris.bootforum2.event.DiscussionCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class ForumStatUpdateListener {

    private static final Logger logger = LoggerFactory.getLogger(ForumStatUpdateListener.class);

    private final GenericDAO genericDAO;

    public ForumStatUpdateListener(GenericDAO genericDAO) {
        this.genericDAO = genericDAO;
    }

    @EventListener
    @Transactional // Make the listener's operation transactional
    @Async // Uncomment for asynchronous execution (requires @EnableAsync in a config class)
    public void handleDiscussionCreatedEvent(DiscussionCreatedEvent event) {
        Discussion discussion = event.getDiscussion();
        String username = event.getCreatorUsername();
        Forum forum = discussion.getForum(); // Assuming Discussion has a getForum()

        if (forum == null) {
            logger.warn("Forum not found for discussion ID {}. Cannot update forum stats.", discussion.getId());
            return;
        }

        // It's safer to re-fetch the forum to ensure we have the latest state
        // especially if the listener is asynchronous.
        Forum managedForum = genericDAO.find(Forum.class, forum.getId());
        if (managedForum == null) {
            logger.warn("Managed Forum not found for discussion ID {}. Cannot update forum stats.", discussion.getId());
            return;
        }

        logger.info("Handling DiscussionCreatedEvent for discussion ID: {}, Forum ID: {}", discussion.getId(), managedForum.getId());

        ForumStat forumStat = managedForum.getStat();
        if (forumStat == null) {
            logger.warn("ForumStat was null for Forum ID {}. Initializing.", managedForum.getId());
            forumStat = new ForumStat(); // Should be handled by @PrePersist in Forum
            managedForum.setStat(forumStat);
        }

        forumStat.addDiscussionCount(1);
        forumStat.addCommentCount(1); // For the initial comment

        // Update: discussion created doesn't count as a comment
        /*Comment initialComment = discussion.getComments().stream().findFirst().orElse(null); // Get the initial comment

        if (initialComment != null && initialComment.getId() != null) {
            CommentInfo forumLastComment = forumStat.getLastComment();
            if (forumLastComment == null) {
                logger.warn("ForumStat.lastComment was null for Forum ID {}. Initializing.", managedForum.getId());
                forumLastComment = new CommentInfo(); // Should be handled by @PrePersist in ForumStat
                forumStat.setLastComment(forumLastComment);
            }

            LocalDateTime initialCommentDate = initialComment.getCreateDate() != null ? initialComment.getCreateDate() : LocalDateTime.now();

            if (forumLastComment.getCommentDate() == null || initialCommentDate.isAfter(forumLastComment.getCommentDate())) {
                forumLastComment.setCommentId(initialComment.getId());
                forumLastComment.setTitle(discussion.getTitle());
                forumLastComment.setCommentor(username);
                forumLastComment.setCommentDate(initialCommentDate);
            }
        } else {
            logger.warn("Initial comment or its ID not found for discussion ID {}. Last comment in forum stat might not be updated.", discussion.getId());
        }*/

        genericDAO.merge(managedForum); // Persist changes to the forum and its stat
        logger.info("Forum stats updated for Forum ID: {}", managedForum.getId());
    }
}