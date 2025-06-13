package com.github.chipolaris.bootforum2.listener;

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.domain.ForumStat;
import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.domain.UserStat;
import com.github.chipolaris.bootforum2.event.DiscussionCreatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DiscussionCreatedListener {

    private final GenericDAO genericDAO;
    private final DynamicDAO dynamicDAO;

    public DiscussionCreatedListener(GenericDAO genericDAO, DynamicDAO dynamicDAO) {
        this.genericDAO = genericDAO;
        this.dynamicDAO = dynamicDAO;
    }

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
        userStat.addThumbnailCount(discussion.getThumbnails().size());
        userStat.addAttachmentCount(discussion.getAttachments().size());
        genericDAO.merge(userStat);
    }

    private void updateForumStat(ForumStat forumStat, Discussion discussion) {
        forumStat.addDiscussionCount(1);
        forumStat.setLastComment(discussion.getStat().getLastComment());
        genericDAO.merge(forumStat);
    }
}
