package com.github.chipolaris.bootforum2.listener;

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.*;
import com.github.chipolaris.bootforum2.event.DiscussionCreatedEvent;
import com.github.chipolaris.bootforum2.event.DiscussionViewedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Component to listen to Discussion events
 */
@Component
public class DiscussionEventsListener {

    private final GenericDAO genericDAO;
    private final DynamicDAO dynamicDAO;

    public DiscussionEventsListener(GenericDAO genericDAO, DynamicDAO dynamicDAO) {
        this.genericDAO = genericDAO;
        this.dynamicDAO = dynamicDAO;
    }

    /**
     * DiscussionCreatedEvent listener
     * @param event
     */
    @EventListener
    @Transactional
    @Async
    public void handleDiscussionCreated(DiscussionCreatedEvent event) {

        String username = event.getCreatorUsername();
        Discussion discussion = event.getDiscussion();

        ForumStat forumStat = discussion.getForum().getStat();

        updateForumStat(forumStat, discussion);

        QuerySpec querySpec = QuerySpec.builder(User.class).filter(FilterSpec.eq("username", username)).build();
        User user = dynamicDAO.<User>findOptional(querySpec).orElse(null);

        UserStat userStat = user.getStat();
        updateUserStat(userStat, discussion);
    }

    private void updateUserStat(UserStat userStat, Discussion discussion) {
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

    private void updateForumStat(ForumStat forumStat, Discussion discussion) {
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
