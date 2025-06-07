package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.ApiResponse;
import com.github.chipolaris.bootforum2.dto.CommentDTO;
import com.github.chipolaris.bootforum2.dto.PageResponseDTO;
import com.github.chipolaris.bootforum2.service.CommentService;
import com.github.chipolaris.bootforum2.service.ServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public") // Base path for public comment-related APIs
public class CommentController {

    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Retrieves a paginated list of comments for a specific discussion.
     *
     * @param discussionId The ID of the discussion whose comments are to be retrieved.
     * @param pageable     Spring Data Pageable object for pagination and sorting.
     *                     Defaults: size=10, sort='createDate' ASC.
     * @return ApiResponse containing a PageResponseDTO of CommentDTOs or error details.
     */
    @GetMapping("/discussion/{discussionId}/comments")
    public ApiResponse<?> listCommentsByDiscussion(
            @PathVariable Long discussionId,
            @PageableDefault(size = 10, sort = "createDate", direction = Sort.Direction.ASC) Pageable pageable) {

        logger.info("Received request to list comments for discussion ID: {}. Pageable: {}", discussionId, pageable);

        if (discussionId == null) {
            logger.warn("discussionId is not provided in request parameters.");
            return ApiResponse.error("discussionId parameter cannot be null.");
        }

        try {
            ServiceResponse<PageResponseDTO<CommentDTO>> serviceResponse =
                    commentService.findPaginatedComments(discussionId, pageable);

            if (serviceResponse.getAckCode() == ServiceResponse.AckCodeType.SUCCESS) {
                return ApiResponse.success(serviceResponse.getDataObject(), "Comments retrieved successfully.");
            } else {
                return ApiResponse.error(serviceResponse.getMessages(), "Failed to retrieve comments.");
            }
        } catch (Exception e) {
            logger.error(String.format("Unexpected error while listing comments for discussion ID %d: ", discussionId), e);
            return ApiResponse.error("An unexpected error occurred while retrieving comments.");
        }
    }
}