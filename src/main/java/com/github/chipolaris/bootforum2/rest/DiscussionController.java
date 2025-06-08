package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.*;
import com.github.chipolaris.bootforum2.service.DiscussionService;
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
@RequestMapping("/api")
public class DiscussionController {

    private static final Logger logger = LoggerFactory.getLogger(DiscussionController.class);

    private final DiscussionService discussionService;

    public DiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    /**
     * Handles the creation of a new discussion.
     * Expects multipart/form-data.
     *
     * @param discussionCreateDTO DTO containing forumId, title, and comment.
     * @param images              Optional array of image files.
     * @param attachments         Optional array of attachment files.
     * @return ResponseEntity containing the ServiceResponse with the created DiscussionDTO or error details.
     */
    @PostMapping(value = "/user/discussions/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> createDiscussion(
            @Valid @ModelAttribute DiscussionCreateDTO discussionCreateDTO,
            @RequestParam(value = "images", required = false) MultipartFile[] images,
            @RequestParam(value = "attachments", required = false) MultipartFile[] attachments) {

        logger.info("Received request to create discussion: {}", discussionCreateDTO.title());
        if (images != null) {
            logger.info("Number of images received: {}", images.length);
        }
        if (attachments != null) {
            logger.info("Number of attachments received: {}", attachments.length);
        }

        try {
            ServiceResponse<DiscussionDTO> serviceResponse = discussionService.createDiscussion(
                    discussionCreateDTO, images, attachments);

            if (serviceResponse.getAckCode() == ServiceResponse.AckCodeType.FAILURE) {
                return ApiResponse.error(serviceResponse.getMessages(),"Discussion creation failed");
            }
            return ApiResponse.success(serviceResponse.getDataObject(), "Discussion created successfully");
        } catch (Exception e) {
            logger.error("Unexpected create discussion error", e);
            return ApiResponse.error(String.format("An unexpected error occurred during creating discussion.",
                    e.getMessage()));
        }
    }

    /**
     * Retrieves a paginated and sorted list of discussions.
     * Spring automatically populates the Pageable object from request parameters:
     * page: page number (0-indexed)
     * size: page size
     * sort: property,direction (e.g., sort=title,asc or sort=title,asc&sort=author,desc)
     *
     * @param forumId  ID of the forum to filter discussions.
     * @param pageable  Spring Data Pageable object automatically resolved from request parameters.
     * @return ApiResponse containing a Page of DiscussionDTOs or error details.
     */
    @GetMapping("/public/discussions/list")
    public ApiResponse<?> listDiscussions(
            @RequestParam(required = true) Long forumId,
            //@PageableDefault(size = 10, sort = "stat.lastComment.commentDate", direction = Sort.Direction.DESC) Pageable pageable) {
            @PageableDefault(size = 10, sort = "d.createDate", direction = Sort.Direction.DESC) Pageable pageable) {

        logger.info("Received request to list discussions. ForumId: {}, Pageable: {}",
                forumId, pageable);

        try {

            ServiceResponse<PageResponseDTO<DiscussionSummaryDTO>> serviceResponse = discussionService.findPaginatedDiscussionSummaries(
                    forumId, pageable);

            if (serviceResponse.getAckCode() == ServiceResponse.AckCodeType.SUCCESS) {
                return ApiResponse.success(serviceResponse.getDataObject(), "Discussions retrieved successfully.");
            } else {
                return ApiResponse.error(serviceResponse.getMessages(), "Failed to retrieve discussions.");
            }
        } catch (Exception e) {
            logger.error("Unexpected error while listing discussions", e);
            return ApiResponse.error("An unexpected error occurred while retrieving discussions.");
        }
    }

    /**
     * Retrieves a single discussion by its ID.
     *
     * @param discussionId The ID of the discussion to retrieve.
     * @return ApiResponse containing the DiscussionDTO or error details.
     */
    @GetMapping("/public/discussions/{discussionId}")
    public ApiResponse<?> getDiscussion(@PathVariable Long discussionId) {
        logger.info("Received request to get discussion with ID: {}", discussionId);

        if (discussionId == null) {
            // This check is somewhat redundant due to @PathVariable being required,
            // but kept for explicit clarity if the path variable was optional.
            logger.warn("ID is null in path.");
            return ApiResponse.error("ID cannot be null.");
        }

        try {
            ServiceResponse<DiscussionDTO> serviceResponse = discussionService.findDiscussion(discussionId);

            if (serviceResponse.getAckCode() == ServiceResponse.AckCodeType.SUCCESS) {
                return ApiResponse.success(serviceResponse.getDataObject(), "Discussion retrieved successfully.");
            } else {
                return ApiResponse.error(serviceResponse.getMessages(), String.format("Failed to retrieve discussion with ID %d.", discussionId));
            }
        } catch (Exception e) {
            logger.error(String.format("Unexpected error while retrieving discussion with ID %d", discussionId), e);
            return ApiResponse.error(String.format("An unexpected error occurred while retrieving discussion with ID %d.", discussionId));
        }
    }
}