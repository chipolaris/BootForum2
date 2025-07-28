package com.github.chipolaris.bootforum2.listener;

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.*;
import com.github.chipolaris.bootforum2.event.DiscussionCreatedEvent;
import com.github.chipolaris.bootforum2.event.DiscussionViewedEvent;
import com.github.chipolaris.bootforum2.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Component to listen to Discussion events
 */
@Component
public class DiscussionEventsListener {

    private static final Logger logger = LoggerFactory.getLogger(DiscussionEventsListener.class);

    private final GenericDAO genericDAO;
    private final UserRepository userRepository;

    public DiscussionEventsListener(GenericDAO genericDAO, UserRepository userRepository) {
        this.genericDAO = genericDAO;
        this.userRepository = userRepository;
    }

    /**
     * DiscussionCreatedEvent listener
     * @param event
     */
    @EventListener
    @Transactional
    @Async
    public void handleDiscussionCreated(DiscussionCreatedEvent event) {
        // When a discussion is created
        Discussion discussion = event.getDiscussion();

        updateDiscussionStat(discussion);
        updateForumStat(discussion);
        updateUserStat(discussion);
    }

    private void updateDiscussionStat(Discussion discussion) {
        DiscussionStat discussionStat = discussion.getStat();

        discussionStat.addParticipant(discussion.getCreateBy());

        if (discussion.getImages() != null) {
            discussionStat.setImageCount(discussion.getImages().size());
        }
        if (discussion.getAttachments() != null) {
            discussionStat.setAttachmentCount(discussion.getAttachments().size());
        }

        genericDAO.merge(discussionStat);
    }

    private void updateUserStat(Discussion discussion) {

        String username = discussion.getCreateBy();
        Optional<User> userOpt = userRepository.findByUsername(username);

        if(userOpt.isEmpty()) {
            logger.warn("User not found for username: {}", username);
            return; // short circuit if user not found
        }

        User user = userOpt.get();

        UserStat userStat = user.getStat();
        userStat.addDiscussionCount(1);
        userStat.addImageCount(discussion.getImages().size());
        userStat.addAttachmentCount(discussion.getAttachments().size());
        
        DiscussionInfo lastDiscussion = userStat.getLastDiscussion();
        if(lastDiscussion.getDiscussionCreateDate() == null ||
                lastDiscussion.getDiscussionCreateDate().isBefore(discussion.getCreateDate())) {

            lastDiscussion.setDiscussionId(discussion.getId());
            lastDiscussion.setDiscussionCreateDate(discussion.getCreateDate());
            lastDiscussion.setDiscussionCreator(discussion.getCreateBy());
            lastDiscussion.setTitle(discussion.getTitle());
            lastDiscussion.setContentAbbr(discussion.getContent().substring(0, Math.min(discussion.getContent().length(), 255)));
        }
        
        genericDAO.merge(userStat);
    }

    private void updateForumStat(Discussion discussion) {
        ForumStat forumStat = discussion.getForum().getStat();
        forumStat.addDiscussionCount(1);

        DiscussionInfo discussionInfo = forumStat.getLastDiscussion();
        if(discussionInfo.getDiscussionCreateDate() == null ||
                discussionInfo.getDiscussionCreateDate().isBefore(discussion.getCreateDate())) {
            discussionInfo.setDiscussionId(discussion.getId());
            discussionInfo.setDiscussionCreateDate(discussion.getCreateDate());

            discussionInfo.setDiscussionCreator(discussion.getCreateBy());
            discussionInfo.setTitle(discussion.getTitle());
            discussionInfo.setContentAbbr(discussion.getContent().substring(0, Math.min(discussion.getContent().length(), 255)));
        }

        genericDAO.merge(forumStat);
    }

    /**
     * DiscussionViewedEvent listener
     * @param event
     */
    @EventListener
    @Transactional
    @Async
    public void handleDiscussionViewed(DiscussionViewedEvent event) {
        Discussion discussion = event.getDiscussion();
        discussion.getStat().addViewCount(1);
        genericDAO.merge(discussion.getStat());
    }
}
