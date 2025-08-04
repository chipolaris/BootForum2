package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.domain.*;
import com.github.chipolaris.bootforum2.repository.CommentRepository;
import com.github.chipolaris.bootforum2.repository.DiscussionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class StatService {

    private static final Logger logger = LoggerFactory.getLogger(StatService.class);

    private final GenericDAO genericDAO;
    private final CommentRepository commentRepository;
    private final DiscussionRepository discussionRepository;

    // Note: in Spring version >= 4.3, @AutoWired is implied for beans with single constructor
    public StatService(GenericDAO genericDAO, CommentRepository commentRepository,
                       DiscussionRepository discussionRepository) {
        this.genericDAO = genericDAO;
        this.commentRepository = commentRepository;
        this.discussionRepository = discussionRepository;
    }

    //-----
    @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    public ServiceResponse<DiscussionStat> syncDiscussionStat(Discussion discussion) {

        return ServiceResponse.success("Discussion Stat refreshed",
                refreshDiscussionStatFromDB(discussion));
    }

    private DiscussionStat refreshDiscussionStatFromDB(Discussion discussion) {

        DiscussionStat discussionStat = discussion.getStat();

        discussionStat.setCommentCount(commentRepository.countByDiscussion(discussion));

        /* refresh commentors map */
        discussionStat.setParticipants(commentRepository.getCommentorMap(discussion));

        discussionStat.setImageCount( (discussion.getImages() != null ? discussion.getImages().size() : 0)
                + commentRepository.countImagesByDiscussion(discussion));
        discussionStat.setAttachmentCount((discussion.getAttachments() != null ? discussion.getAttachments().size() : 0)
                + commentRepository.countAttachmentsByDiscussion(discussion));

        Optional<Comment> lastCommentOpt = commentRepository.findTopByDiscussionOrderByCreateDateDesc(discussion);
        if (lastCommentOpt.isPresent()) {
            CommentInfo commentInfo = discussionStat.getLastComment();
            updateCommentInfo(commentInfo, lastCommentOpt.get());

            // note: even though discussionStat.lastComment is configured as Cascade.ALL
            // we still need this explicit merge (save) here because discussionStat
            // might not call saved itself because of dirty-tracking (if it's not updated)
            genericDAO.merge(commentInfo);
        }

        genericDAO.merge(discussionStat);

        return discussionStat;
    }

    private void updateCommentInfo(CommentInfo commentInfo, Comment comment) {
        commentInfo.setCommentId(comment.getId());
        commentInfo.setCommentor(comment.getCreateBy());
        commentInfo.setCommentDate(comment.getCreateDate());
        commentInfo.setTitle(comment.getTitle());
        commentInfo.setContentAbbr(comment.getContent());
    }

    // -- sync forum stat
    @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    public ServiceResponse<ForumStat> syncForumStat(Forum forum) {
        return ServiceResponse.success("Forum Stat refreshed", refreshForumStatFromDB(forum));
    }

    private ForumStat refreshForumStatFromDB(Forum forum) {

        ForumStat forumStat = forum.getStat();
        forumStat.setDiscussionCount(discussionRepository.countByForum(forum));
        forumStat.setCommentCount(commentRepository.countByForum(forum));

        Optional<Discussion> lastDicussionOpt = discussionRepository.findTopByForumOrderByCreateDateDesc(forum);
        if (lastDicussionOpt.isPresent()) {
            Discussion lastDiscussion = lastDicussionOpt.get();
            DiscussionInfo discussionInfo = forumStat.getLastDiscussion();
            updateDiscussionInfo(discussionInfo, lastDiscussion);
            genericDAO.merge(discussionInfo);
        }

        Optional<Comment> lastCommentOpt = commentRepository.findLatestByForum(forum);
        if(lastCommentOpt.isPresent()) {
            Comment lastComment = lastCommentOpt.get();
            CommentInfo commentInfo = forumStat.getLastComment();
            updateCommentInfo(commentInfo, lastComment);
            genericDAO.merge(commentInfo);
        }

        genericDAO.merge(forumStat);

        return forumStat;
    }

    private void updateDiscussionInfo(DiscussionInfo discussionInfo, Discussion discussion) {
        discussionInfo.setDiscussionId(discussion.getId());
        discussionInfo.setDiscussionCreator(discussion.getCreateBy());
        discussionInfo.setDiscussionCreateDate(discussionInfo.getCreateDate());
        discussionInfo.setTitle(discussion.getTitle());
        discussionInfo.setContentAbbr(discussion.getContent());
    }
}
