package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.ApiResponse;
import com.github.chipolaris.bootforum2.service.ServiceResponse;
import com.github.chipolaris.bootforum2.service.VoteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/vote") // Base path for user-specific actions
public class VoteController {

    private static final Logger logger = LoggerFactory.getLogger(VoteController.class);

    private final VoteService voteService;

    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping("/comment/{commentId}")
    public ApiResponse<?> voteOnComment(
            @PathVariable Long commentId,
            @RequestParam String voteValue) { // "up" or "down"

        logger.info("Received vote request for comment ID: {}, vote: {}", commentId, voteValue);

        if (commentId == null) {
            return ApiResponse.error("Comment ID cannot be null.");
        }
        if (voteValue == null || (!"up".equalsIgnoreCase(voteValue) && !"down".equalsIgnoreCase(voteValue))) {
            return ApiResponse.error("Vote value must be 'up' or 'down'.");
        }

        try {
            ServiceResponse<Void> serviceResponse = voteService.addVoteToComment(commentId, voteValue);

            if (serviceResponse.isSuccess()) {
                return ApiResponse.success(serviceResponse.getDataObject(), "Vote processed successfully.");
            } else {
                return ApiResponse.error(serviceResponse.getMessages(), "Failed to process vote.");
            }
        } catch (Exception e) {
            logger.error("Unexpected error while processing vote for comment ID %d".formatted(commentId), e);
            return ApiResponse.error("An unexpected error occurred while processing your vote.");
        }
    }

    @PostMapping("/discussion/{discussionId}")
    public ApiResponse<?> voteOnDiscussion(
            @PathVariable Long discussionId,
            @RequestParam String voteValue) { // "up" or "down"

        logger.info("Received vote request for discussion ID: {}, vote: {}", discussionId, voteValue);

        if (discussionId == null) {
            return ApiResponse.error("Discussion ID cannot be null.");
        }
        if (voteValue == null || (!"up".equalsIgnoreCase(voteValue) && !"down".equalsIgnoreCase(voteValue))) {
            return ApiResponse.error("Vote value must be 'up' or 'down'.");
        }

        try {
            ServiceResponse<Void> serviceResponse = voteService.addVoteToDiscussion(discussionId, voteValue);

            if (serviceResponse.isSuccess()) {
                return ApiResponse.success(serviceResponse.getDataObject(), "Vote processed successfully.");
            } else {
                return ApiResponse.error(serviceResponse.getMessages(), "Failed to process vote.");
            }
        } catch (Exception e) {
            logger.error("Unexpected error while processing vote for discussion ID %d".formatted(discussionId), e);
            return ApiResponse.error("An unexpected error occurred while processing your vote.");
        }
    }
}