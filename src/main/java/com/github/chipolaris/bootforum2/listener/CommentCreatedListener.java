package com.github.chipolaris.bootforum2.listener;

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.*;
import com.github.chipolaris.bootforum2.event.CommentCreatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CommentCreatedListener {
    
    private final GenericDAO genericDAO;
    private final DynamicDAO dynamicDAO;
    
    public CommentCreatedListener(GenericDAO genericDAO, DynamicDAO dynamicDAO) {
        this.genericDAO = genericDAO;
        this.dynamicDAO = dynamicDAO;
    }

    @EventListener
    @Transactional
    @Async
    public void handleCommentCreated(CommentCreatedEvent event) {

        Comment comment = event.getComment();
        Discussion discussion = comment.getDiscussion();
        DiscussionStat discussionStat = discussion.getStat();

        updateDiscussionStat(discussionStat, comment);

        Forum forum = discussion.getForum();
        ForumStat forumStat = forum.getStat();

        updateForumStat(forumStat, discussionStat);

        String username = event.getUsername();
        QuerySpec querySpec = QuerySpec.builder(User.class).filter(FilterSpec.eq("username", username)).build();

        User user = dynamicDAO.<User>findOptional(querySpec).orElse(null);

        updateUserStat(user, comment, discussionStat);
    }

    private void updateUserStat(User user, Comment comment, DiscussionStat discussionStat) {
        UserStat userStat = user.getStat();
        userStat.addCommentCount(1);
        userStat.addThumbnailCount(comment.getThumbnails().size());
        userStat.addAttachmentCount(comment.getAttachments().size());
        userStat.setLastComment(discussionStat.getLastComment());
        genericDAO.merge(userStat);
    }

    private void updateForumStat(ForumStat forumStat, DiscussionStat discussionStat) {
        forumStat.addCommentCount(1);
        forumStat.setLastComment(discussionStat.getLastComment());
        genericDAO.merge(forumStat);
    }

    private void updateDiscussionStat(DiscussionStat discussionStat, Comment comment) {
        discussionStat.addCommentCount(1);
        discussionStat.addAttachmentCount(comment.getAttachments().size());
        discussionStat.addThumbnailCount(comment.getThumbnails().size());
        CommentInfo commentInfo = discussionStat.getLastComment();
        commentInfo.setCommentId(comment.getId());
        commentInfo.setCommentor(comment.getCreateBy());
        commentInfo.setCommentDate(comment.getCreateDate());
        commentInfo.setTitle(comment.getTitle());

        commentInfo.setContentAbbr(comment.getContent().substring(0, Math.min(comment.getContent().length(), 255)));
        genericDAO.merge(discussionStat);
    }
}
