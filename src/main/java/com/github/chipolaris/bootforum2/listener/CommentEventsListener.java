package com.github.chipolaris.bootforum2.listener;

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.*;
import com.github.chipolaris.bootforum2.event.CommentCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Component to listen to Comment events
 */
@Component
public class CommentEventsListener {

    private static final Logger logger = LoggerFactory.getLogger(CommentEventsListener.class);

    private final GenericDAO genericDAO;
    private final DynamicDAO dynamicDAO;
    
    public CommentEventsListener(GenericDAO genericDAO, DynamicDAO dynamicDAO) {
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

        updateForumStat(discussion, comment);

        updateUserStat(discussionStat, comment);
    }

    private void updateUserStat(DiscussionStat discussionStat, Comment comment) {

        String username = comment.getCreateBy();

        QuerySpec querySpec = QuerySpec.builder(User.class).filter(FilterSpec.eq("username", username)).build();
        User user = dynamicDAO.<User>findOptional(querySpec).orElse(null);

        if (user == null) {
            logger.warn("User not found for username: {}", username);
            return;
        }

        UserStat userStat = user.getStat();
        userStat.addCommentCount(1);
        userStat.addImageCount(comment.getImages().size());
        userStat.addAttachmentCount(comment.getAttachments().size());

        CommentInfo lastComment = userStat.getLastComment();
        if(lastComment.getCommentDate() == null ||
                lastComment.getCommentDate().isBefore(comment.getCreateDate())) {

            lastComment.setCommentId(comment.getId());
            lastComment.setCommentor(comment.getCreateBy());
            lastComment.setCommentDate(comment.getCreateDate());
            lastComment.setTitle(comment.getTitle());
            lastComment.setContentAbbr(comment.getContent().substring(0, Math.min(comment.getContent().length(), 255)));
        }

        genericDAO.merge(userStat);
    }

    private void updateForumStat(Discussion discussion, Comment comment) {

        Forum forum = discussion.getForum();
        // since discussion.forum is LAZY loaded, call merge() to reattach Forum instance to db session...
        genericDAO.merge(forum);
        ForumStat forumStat = forum.getStat();

        forumStat.addCommentCount(1);

        CommentInfo lastComment = forumStat.getLastComment();
        if(lastComment.getCommentDate() == null ||
                lastComment.getCommentDate().isBefore(comment.getCreateDate())) {

            lastComment.setCommentId(comment.getId());
            lastComment.setCommentor(comment.getCreateBy());
            lastComment.setCommentDate(comment.getCreateDate());
            lastComment.setTitle(comment.getTitle());
            lastComment.setContentAbbr(comment.getContent().substring(0, Math.min(comment.getContent().length(), 255)));
        }

        // save update
        genericDAO.merge(forumStat);
    }

    private void updateDiscussionStat(DiscussionStat discussionStat, Comment comment) {
        discussionStat.addCommentCount(1);
        discussionStat.addAttachmentCount(comment.getAttachments().size());
        discussionStat.addImageCount(comment.getImages().size());
        discussionStat.addParticipant(comment.getCreateBy());

        CommentInfo lastComment = discussionStat.getLastComment();

        if(lastComment.getCommentDate() == null ||
                lastComment.getCommentDate().isBefore(comment.getCreateDate())) {
            lastComment.setCommentId(comment.getId());
            lastComment.setCommentor(comment.getCreateBy());
            lastComment.setCommentDate(comment.getCreateDate());
            lastComment.setTitle(comment.getTitle());
            lastComment.setContentAbbr(comment.getContent().substring(0, Math.min(comment.getContent().length(), 255)));
        }

        genericDAO.merge(discussionStat); // saving discussionStat will cascade to lastComment and participants
    }
}
