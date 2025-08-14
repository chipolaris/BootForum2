package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.*;
import com.github.chipolaris.bootforum2.event.CommentVotedEvent;
import com.github.chipolaris.bootforum2.event.DiscussionVotedEvent;
import com.github.chipolaris.bootforum2.repository.CommentVoteRepository;
import com.github.chipolaris.bootforum2.repository.DiscussionStatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;

@Service
public class VoteService {

    private static final Logger logger = LoggerFactory.getLogger(VoteService.class);

    private final GenericDAO genericDAO;
    private final CommentVoteRepository commentVoteRepository;
    private final DiscussionStatRepository discussionStatRepository;
    private final AuthenticationFacade authenticationFacade;
    private final ApplicationEventPublisher eventPublisher;

    public VoteService(GenericDAO genericDAO, CommentVoteRepository commentVoteRepository,
                       DiscussionStatRepository discussionStatRepository, AuthenticationFacade authenticationFacade,
                       ApplicationEventPublisher eventPublisher) {
        this.genericDAO = genericDAO;
        this.commentVoteRepository = commentVoteRepository;
        this.discussionStatRepository = discussionStatRepository;
        this.authenticationFacade = authenticationFacade;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = false)
    public ServiceResponse<Void> addVoteToComment(Long commentId, String voteValueInput) {

        // Ensure user is logged in and get their username
        Optional<String> currentUsernameOpt = authenticationFacade.getCurrentUsername();
        if (currentUsernameOpt.isEmpty()) {
            return ServiceResponse.failure("User must be logged in to vote.");
        }
        String currentUsername = currentUsernameOpt.get();

        short voteValue = 0;
        if ("up".equalsIgnoreCase(voteValueInput)) {
            voteValue = 1;
        } else if ("down".equalsIgnoreCase(voteValueInput)) {
            voteValue = -1;
        } else {
            return ServiceResponse.failure("Invalid vote value. Must be 'up' or 'down'.");
        }

        Comment comment = genericDAO.find(Comment.class, commentId);
        if (comment == null) {
            return ServiceResponse.failure("Comment with ID %d not found.".formatted(commentId));
        }

        CommentVote commentVote = comment.getCommentVote();

        if(commentVote == null) {
            commentVote = new CommentVote();
            comment.setCommentVote(commentVote);
            //genericDAO.merge(comment);
        }

        if (commentVote.getVotes() == null) {
            commentVote.setVotes(new HashSet<>());
        }

        // Check if the user has already voted
        boolean alreadyVoted = commentVoteRepository.hasUserVotedOnCommentVote(commentVote.getId(), currentUsername);
        if (alreadyVoted) {
            return ServiceResponse.failure("You have already voted on this comment.");
        }

        // Create and add the new vote
        Vote newVote = new Vote();
        newVote.setVoterName(currentUsername);
        newVote.setVoteValue(voteValue);

        commentVote.getVotes().add(newVote); // Add to the collection in CommentVote

        // Update counts
        if (voteValue > 0) {
            commentVote.addVoteUpCount();
        } else {
            commentVote.addVoteDownCount();
        }

        try {
            // Persisting the Vote is handled by the CascadeType.ALL on Comment.commentVote.
            // Merging the parent comment is sufficient to persist the changes to CommentVote and the new Vote.
            genericDAO.merge(comment);

            logger.info("User '{}' voted '{}' on comment ID {}", currentUsername, voteValue, commentId);

            // Publish event
            eventPublisher.publishEvent(new CommentVotedEvent(this, comment, currentUsername, voteValue));

            return ServiceResponse.success("Vote registered successfully.");
        } catch (Exception e) {
            logger.error("Error while registering vote for user '%s' on comment ID %d".formatted(currentUsername, commentId), e);

            // Rollback will happen due to @Transactional
            return ServiceResponse.failure("An error occurred while registering your vote.");
        }
    }

    @Transactional(readOnly = false)
    public ServiceResponse<Void> addVoteToDiscussion(Long discussionId, String voteValueInput) {

        // Ensure user is logged in and get their username
        Optional<String> currentUsernameOpt = authenticationFacade.getCurrentUsername();
        if (currentUsernameOpt.isEmpty()) {
            return ServiceResponse.failure("User must be logged in to vote.");
        }
        String currentUsername = currentUsernameOpt.get();

        short voteValue = 0;
        if ("up".equalsIgnoreCase(voteValueInput)) {
            voteValue = 1;
        } else if ("down".equalsIgnoreCase(voteValueInput)) {
            voteValue = -1;
        } else {
            return ServiceResponse.failure("Invalid vote value. Must be 'up' or 'down'.");
        }

        Discussion discussion = genericDAO.find(Discussion.class, discussionId);
        if (discussion == null) {
            return ServiceResponse.failure("Discussion with ID %d not found.".formatted(discussionId));
        }

        DiscussionStat discussionStat = discussion.getStat();

        if (discussionStat.getVotes() == null) {
            discussionStat.setVotes(new HashSet<>());
        }

        // Check if the user has already voted
        boolean alreadyVoted = discussionStatRepository.hasUserVotedOnDiscussionStat(discussionStat.getId(), currentUsername);
        if (alreadyVoted) {
            return ServiceResponse.failure("You have already voted on this discussion.");
        }

        // Create and add the new vote
        Vote newVote = new Vote();
        newVote.setVoterName(currentUsername);
        newVote.setVoteValue(voteValue);

        discussionStat.getVotes().add(newVote); // Add to the collection in CommentVote

        // Update counts
        if (voteValue > 0) {
            discussionStat.addVoteUpCount();
        } else {
            discussionStat.addVoteDownCount();
        }

        try {
            // Persisting the Vote entity explicitly if it's not cascaded from CommentVote,
            // or if CommentVote's cascade doesn't cover adding to the 'votes' collection effectively.
            // If CommentVote.votes has CascadeType.ALL, merging commentVote (or comment) should be enough.
            genericDAO.persist(newVote); // Persist the new Vote entity
            genericDAO.merge(discussionStat); // Merge CommentVote to update counts and the collection
            // If DiscussionStat is managed by Discussion's cascade, merging Comment is the primary way.
            // genericDAO.merge(comment);

            logger.info("User '{}' voted '{}' on comment ID {}", currentUsername, voteValue, discussionId);

            // Publish event
            eventPublisher.publishEvent(new DiscussionVotedEvent(this, discussion, currentUsername, voteValue));

            return ServiceResponse.success("Vote registered successfully.");
        } catch (Exception e) {
            logger.error("Error while registering vote for user '%s' on discussion ID %d".formatted(currentUsername, discussionId), e);

            // Rollback will happen due to @Transactional
            return ServiceResponse.failure("An error occurred while registering your vote.");
        }
    }
}