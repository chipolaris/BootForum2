package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.Comment;
import com.github.chipolaris.bootforum2.domain.CommentVote;
import com.github.chipolaris.bootforum2.domain.Vote;
import com.github.chipolaris.bootforum2.event.CommentVotedEvent;
import com.github.chipolaris.bootforum2.repository.CommentVoteRepository;
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
    private final AuthenticationFacade authenticationFacade;
    private final ApplicationEventPublisher eventPublisher;

    public VoteService(GenericDAO genericDAO, CommentVoteRepository commentVoteRepository,
                       AuthenticationFacade authenticationFacade, ApplicationEventPublisher eventPublisher) {
        this.genericDAO = genericDAO;
        this.commentVoteRepository = commentVoteRepository;
        this.authenticationFacade = authenticationFacade;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ServiceResponse<Void> addVoteToComment(Long commentId, String voteValueInput) {
        ServiceResponse<Void> response = new ServiceResponse<>();

        Optional<String> currentUsernameOpt = authenticationFacade.getCurrentUsername();
        if (currentUsernameOpt.isEmpty()) {
            return response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage("User must be logged in to vote.");
        }
        String currentUsername = currentUsernameOpt.get();

        short voteValue = 0;
        if ("up".equalsIgnoreCase(voteValueInput)) {
            voteValue = 1;
        } else if ("down".equalsIgnoreCase(voteValueInput)) {
            voteValue = -1;
        } else {
            return response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage("Invalid vote value. Must be 'up' or 'down'.");
        }

        Comment comment = genericDAO.find(Comment.class, commentId);
        if (comment == null) {
            return response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage(String.format("Comment with ID %d not found.", commentId));
        }

        CommentVote commentVote = comment.getCommentVote();
        if (commentVote == null) {
            commentVote = new CommentVote();
            comment.setCommentVote(commentVote);
            // Ensure CommentVote is persisted if it's new.
            // If CommentVote has its own ID generator and is not purely cascaded for persist,
            // you might need to persist it separately or ensure cascade settings are correct.
            // For now, assuming cascade from Comment handles this.
        }

        if (commentVote.getVotes() == null) {
            commentVote.setVotes(new HashSet<>());
        }

        // Check if the user has already voted
        boolean alreadyVoted = commentVoteRepository.hasUserVotedOnCommentVote(commentVote.getId(), currentUsername);
        if (alreadyVoted) {
            return response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage("You have already voted on this comment.");
        }

        // Create and add the new vote
        Vote newVote = new Vote();
        newVote.setVoterName(currentUsername);
        newVote.setVoteValue(voteValue);

        commentVote.getVotes().add(newVote); // Add to the collection in CommentVote

        // Update counts
        if (voteValue == 1) {
            commentVote.setVoteUpCount(commentVote.getVoteUpCount() + 1);
        } else {
            commentVote.setVoteDownCount(commentVote.getVoteDownCount() + 1);
        }

        try {
            // Persisting the Vote entity explicitly if it's not cascaded from CommentVote,
            // or if CommentVote's cascade doesn't cover adding to the 'votes' collection effectively.
            // If CommentVote.votes has CascadeType.ALL, merging commentVote (or comment) should be enough.
            genericDAO.persist(newVote); // Persist the new Vote entity
            genericDAO.merge(commentVote); // Merge CommentVote to update counts and the collection
            // If CommentVote is managed by Comment's cascade, merging Comment is the primary way.
            // genericDAO.merge(comment);

            logger.info("User '{}' voted '{}' on comment ID {}", currentUsername, voteValue, commentId);

            // Publish event
            eventPublisher.publishEvent(new CommentVotedEvent(this, comment, currentUsername, voteValue));

            return response;
        } catch (Exception e) {
            logger.error(String.format("Error while registering vote for user '%s' on comment ID %d", currentUsername, commentId), e);
            // Rollback will happen due to @Transactional
            return response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage("An error occurred while registering your vote.");
        }
    }
}