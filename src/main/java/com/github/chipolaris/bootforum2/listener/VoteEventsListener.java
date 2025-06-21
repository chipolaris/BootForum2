package com.github.chipolaris.bootforum2.listener;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.domain.Comment;
import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.event.CommentVotedEvent;
import com.github.chipolaris.bootforum2.event.DiscussionVotedEvent;
import com.github.chipolaris.bootforum2.repository.UserStatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class VoteEventsListener {

    private static final Logger logger = LoggerFactory.getLogger(VoteEventsListener.class);

    private final GenericDAO genericDAO;
    private final UserStatRepository userStatRepository;

    public VoteEventsListener(GenericDAO genericDAO, UserStatRepository userStatRepository) {
        this.genericDAO = genericDAO;
        this.userStatRepository = userStatRepository;
    }

    @EventListener
    @Transactional(readOnly = false) // Ensure the update is part of a transaction
    @Async
    public void handleDiscussionVotedEvent(DiscussionVotedEvent event) {

        logger.info("Handling DiscussionVoteEvent %s".formatted(event.toString()));

        Short voteValue = event.getVoteValue();
        Discussion discussion = event.getDiscussion();
        String discussionCreator = discussion.getCreateBy();

        userStatRepository.addReputationByUsername(discussionCreator, voteValue);
    }

    @EventListener
    @Transactional(readOnly = false) // Ensure the update is part of a transaction
    @Async
    public void handleCommentVotedEvent(CommentVotedEvent event) {

        logger.info("Handling CommentVoteEvent %s".formatted(event.toString()));

        Short voteValue = event.getVoteValue();
        Comment comment = event.getComment();
        String commentor = comment.getCreateBy();

        userStatRepository.addReputationByUsername(commentor, voteValue);
    }
}
