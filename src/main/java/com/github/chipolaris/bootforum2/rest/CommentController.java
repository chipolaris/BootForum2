package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.domain.Comment;
import com.github.chipolaris.bootforum2.dto.*;
import com.github.chipolaris.bootforum2.mapper.CommentMapper;
import com.github.chipolaris.bootforum2.service.CommentService;
import com.github.chipolaris.bootforum2.service.GenericService;
import com.github.chipolaris.bootforum2.service.ServiceResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api") // Base path for public comment-related APIs
public class CommentController {

    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);

    private final CommentService commentService;
    private final CommentMapper commentMapper;
    private final GenericService genericService;

    public CommentController(GenericService genericService, CommentService commentService, CommentMapper commentMapper) {
        this.genericService = genericService;
        this.commentService = commentService;
        this.commentMapper = commentMapper;
    }

    /**
     * NEW: Performs a full-text search for comments.
     *
     * @param keyword  The keyword to search for in comment titles and content.
     * @param pageable Spring Data Pageable object for pagination and sorting.
     * @return ApiResponse containing a PageResponseDTO of matching CommentDTOs or error details.
     */
    @GetMapping("/public/comments/search")
    public ApiResponse<?> searchComments(
            @RequestParam("keyword") String keyword,
            @PageableDefault(size = 10, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable) {

        logger.info("Received request to search comments with keyword: '{}'", keyword);

        if (keyword == null || keyword.isBlank()) {
            return ApiResponse.error("Search keyword cannot be empty.");
        }

        try {
            ServiceResponse<PageResponseDTO<CommentDTO>> serviceResponse =
                    commentService.searchComments(keyword, pageable);

            if (serviceResponse.isSuccess()) {
                return ApiResponse.success(serviceResponse.getDataObject(), "Search completed successfully.");
            } else {
                return ApiResponse.error(serviceResponse.getMessages(), "Failed to perform search.");
            }
        } catch (Exception e) {
            logger.error(String.format("Unexpected error while searching comments for keyword '%s': ", keyword), e);
            return ApiResponse.error("An unexpected error occurred during the search.");
        }
    }

    /**
     * Retrieves a paginated list of comments for a specific discussion.
     *
     * @param discussionId The ID of the discussion whose comments are to be retrieved.
     * @param pageable     Spring Data Pageable object for pagination and sorting.
     *                     Defaults: size=10, sort='createDate' ASC.
     * @return ApiResponse containing a PageResponseDTO of CommentDTOs or error details.
     */
    @GetMapping("/public/comments/by-discussion/{discussionId}")
    public ApiResponse<?> listCommentsByDiscussion(
            @PathVariable Long discussionId,
            @PageableDefault(size = 10, sort = "createDate", direction = Sort.Direction.ASC) Pageable pageable) {

        logger.info("Received request to list comments for discussion ID: {}. Pageable: {}", discussionId, pageable);

        // @PathVariable ensures discussionId is not null if path is matched
        try {
            ServiceResponse<PageResponseDTO<CommentDTO>> serviceResponse =
                    commentService.findPaginatedComments(discussionId, pageable);

            if (serviceResponse.isSuccess()) {
                return ApiResponse.success(serviceResponse.getDataObject(), "Comments retrieved successfully.");
            } else {
                return ApiResponse.error(serviceResponse.getMessages(), "Failed to retrieve comments.");
            }
        } catch (Exception e) {
            logger.error(String.format("Unexpected error while listing comments for discussion ID %d: ", discussionId), e);
            return ApiResponse.error("An unexpected error occurred while retrieving comments.");
        }
    }

    /**
     * Handles the creation of a new comment.
     * Expects multipart/form-data.
     *
     * @param commentCreateDTO DTO containing discussionId, replyToId, title, and content.
     * @param images           Optional array of image files.
     * @param attachments      Optional array of attachment files.
     * @return ApiResponse containing the created CommentDTO or error details.
     */
    @PostMapping(value = "/user/comments/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> createComment(
            @Valid @ModelAttribute CommentCreateDTO commentCreateDTO,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "attachments", required = false) MultipartFile[] attachments) {

        logger.info("Received request to create comment: {}", commentCreateDTO.title());
        if (images != null) {
            logger.info("Number of images received for comment: {}", images.length);
        }
        if (attachments != null) {
            logger.info("Number of attachments received for comment: {}", attachments.length);
        }

        try {
            ServiceResponse<CommentDTO> serviceResponse = commentService.createComment(
                    commentCreateDTO, images, attachments);

            if (serviceResponse.isFailure()) {
                return ApiResponse.error(serviceResponse.getMessages(), "Comment creation failed");
            }
            return ApiResponse.success(serviceResponse.getDataObject(), "Comment created successfully");
        } catch (Exception e) {
            logger.error("Unexpected create comment error for title: " + commentCreateDTO.title(), e);
            return ApiResponse.error("An unexpected error occurred during comment creation: " + e.getMessage());
        }
    }

    @GetMapping("/public/comments/{commentId}")
    public ApiResponse<?> getComment(@PathVariable Long commentId) {
        logger.info("Received request to get comment with ID: {}", commentId);

        try {
            ServiceResponse<CommentDTO> serviceResponse =
                genericService.findEntityDTO(Comment.class, commentId, commentMapper::toCommentDTO);

            if(serviceResponse.isFailure()) {
                return ApiResponse.error(serviceResponse.getMessages(), "Failed to retrieve comment.");
            }
            return ApiResponse.success(serviceResponse.getDataObject(), "Comment retrieved successfully.");
        }
        catch (Exception e) {
            logger.error("Unexpected error while getting comment with ID: " + commentId, e);
            return ApiResponse.error("An unexpected error occurred while retrieving the comment.");
        }
    }

    @GetMapping("/public/comments/{commentId}/thread")
    public ApiResponse<?> getCommentThread(@PathVariable Long commentId) {
        logger.info("Received request to get comment thread for comment ID: {}", commentId);

        try {
            ServiceResponse<CommentThreadDTO> serviceResponse = commentService.getCommentThread(commentId);

            if (serviceResponse.isFailure()) {
                return ApiResponse.error(serviceResponse.getMessages(), "Failed to retrieve comment");
            }
            return ApiResponse.success(serviceResponse.getDataObject(), "Comment thread retrieved successfully.");
        } catch (Exception e) {
            logger.error("Unexpected error while getting comment thread for comment id {}", commentId);
            return ApiResponse.error("An unexpected error occurred while retrieving the comment thread.");
        }
    }
}