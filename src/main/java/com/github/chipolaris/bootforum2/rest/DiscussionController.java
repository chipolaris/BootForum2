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

import java.util.List;

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

            if (serviceResponse.isFailure()) {
                return ApiResponse.error(serviceResponse.getMessages(),"Discussion creation failed");
            }
            return ApiResponse.success(serviceResponse.getDataObject(), "Discussion created successfully");
        } catch (Exception e) {
            logger.error("Unexpected create discussion error", e);
            return ApiResponse.error(String.format("An unexpected error occurred during creating discussion.",
                    e.getMessage()));
        }
    }

    @GetMapping("/public/discussions/list")
    public ApiResponse<?> listDiscussions(
            @RequestParam(required = false) Long forumId,
            @PageableDefault(size = 10, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable) {

        logger.info("Received request to list discussions. ForumId: {}, Pageable: {}",
                forumId == null ? "all" : forumId, pageable);

        try {
            ServiceResponse<PageResponseDTO<DiscussionSummaryDTO>> serviceResponse;

            if (forumId != null) {
                // If forumId is provided, get discussions for that specific forum
                serviceResponse = discussionService.findPaginatedDiscussionSummariesForForum(forumId, pageable);
            } else {
                // Otherwise, get all discussions
                serviceResponse = discussionService.findPaginatedDiscussionSummaries(pageable);
            }

            if (serviceResponse.isSuccess()) {
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
     * Lists discussions filtered by a list of tag IDs.
     * @param tagIds A list of tag IDs to filter discussions by.
     * @param pageable Pageable object, defaults to sorting by createDate descending.
     * @return ApiResponse containing a paginated list of discussion summaries.
     */
    @GetMapping("/public/discussions/by-tags")
    public ApiResponse<?> listDiscussionsByTags(
            @RequestParam List<Long> tagIds,
            @PageableDefault(size = 25, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable) {

        logger.info("Received request to list discussions by tags. TagIds: {}, Pageable: {}", tagIds, pageable);

        try {
            ServiceResponse<PageResponseDTO<DiscussionSummaryDTO>> serviceResponse =
                    discussionService.findPaginatedDiscussionSummariesForTags(tagIds, pageable);

            if (serviceResponse.isSuccess()) {
                return ApiResponse.success(serviceResponse.getDataObject(), "Discussions for tags retrieved successfully.");
            } else {
                return ApiResponse.error(serviceResponse.getMessages(), "Failed to retrieve discussions for tags.");
            }
        } catch (Exception e) {
            logger.error("Unexpected error while listing discussions by tags", e);
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
            ServiceResponse<DiscussionDTO> serviceResponse = discussionService.getDiscussion(discussionId);

            if (serviceResponse.isSuccess()) {
                return ApiResponse.success(serviceResponse.getDataObject(), "Discussion retrieved successfully.");
            } else {
                return ApiResponse.error(serviceResponse.getMessages(), String.format("Failed to retrieve discussion with ID %d.", discussionId));
            }
        } catch (Exception e) {
            logger.error(String.format("Unexpected error while retrieving discussion with ID %d", discussionId), e);
            return ApiResponse.error(String.format("An unexpected error occurred while retrieving discussion with ID %d.", discussionId));
        }
    }

    /**
     * NEW: Retrieves a list of discussions similar to the given one.
     * @param discussionId The ID of the source discussion.
     * @return ApiResponse containing a list of DiscussionSummaryDTOs.
     */
    @GetMapping("/public/discussions/{discussionId}/similar")
    public ApiResponse<?> getSimilarDiscussions(@PathVariable Long discussionId) {
        logger.info("Received request to get similar discussions for ID: {}", discussionId);

        try {
            ServiceResponse<List<DiscussionSummaryDTO>> response = discussionService.findSimilarDiscussions(discussionId, 10);

            if (response.isSuccess()) {
                return ApiResponse.success(response.getDataObject(), "Similar discussions retrieved successfully.");
            } else {
                return ApiResponse.error(response.getMessages(), "Failed to retrieve similar discussions.");
            }
        } catch (Exception e) {
            logger.error("Unexpected error while getting similar discussions for ID " + discussionId, e);
            return ApiResponse.error("An unexpected error occurred while retrieving similar discussions.");
        }
    }

    @GetMapping("/public/discussions/latest")
    public ApiResponse<?> getLatestDiscussions() {
        logger.info("Received request to get latest discussions");
        try {
            ServiceResponse<List<DiscussionDTO>> serviceResponse = discussionService.getLatestDiscussions(5);
            if (serviceResponse.isSuccess()) {
                return ApiResponse.success(serviceResponse.getDataObject(), "Latest discussions retrieved successfully.");
            } else {
                return ApiResponse.error(serviceResponse.getMessages(), "Failed to retrieve latest discussions.");
            }
        } catch (Exception e) {
            logger.error("Unexpected error while getting latest discussions", e);
            return ApiResponse.error("An unexpected error occurred while retrieving latest discussions.");
        }
    }

    @GetMapping("/public/discussions/most-commented")
    public ApiResponse<?> getMostCommentedDiscussions() {
        logger.info("Received request to get most commented discussions");
        try {
            ServiceResponse<List<DiscussionDTO>> serviceResponse = discussionService.getMostCommentedDiscussions(5);
            if (serviceResponse.isSuccess()) {
                return ApiResponse.success(serviceResponse.getDataObject(), "Most commented discussions retrieved successfully.");
            } else {
                return ApiResponse.error(serviceResponse.getMessages(), "Failed to retrieve most commented discussions.");
            }
        } catch (Exception e) {
            logger.error("Unexpected error while getting most commented discussions", e);
            return ApiResponse.error("An unexpected error occurred while retrieving most commented discussions.");
        }
    }

    @GetMapping("/public/discussions/most-viewed")
    public ApiResponse<?> getMostViewedDiscussions() {
        logger.info("Received request to get most viewed discussions");
        try {
            ServiceResponse<List<DiscussionDTO>> serviceResponse = discussionService.getMostViewedDiscussions(5);
            if (serviceResponse.isSuccess()) {
                return ApiResponse.success(serviceResponse.getDataObject(), "Most viewed discussions retrieved successfully.");
            } else {
                return ApiResponse.error(serviceResponse.getMessages(), "Failed to retrieve most viewed discussions.");
            }
        } catch (Exception e) {
            logger.error("Unexpected error while getting most viewed discussions", e);
            return ApiResponse.error("An unexpected error occurred while retrieving most viewed discussions.");
        }
    }

    /**
     * Performs a full-text search for discussions.
     *
     * @param keyword  The keyword to search for in discussion titles and content.
     * @param pageable Spring Data Pageable object for pagination. Note: Sorting is handled by relevance in the service.
     * @return ApiResponse containing a PageResponseDTO of matching DiscussionInfoDTOs.
     */
    @GetMapping("/public/discussions/search")
    public ApiResponse<?> searchDiscussions(
            @RequestParam("keyword") String keyword,
            @PageableDefault(size = 10) Pageable pageable) {

        logger.info("Received request to search discussions with keyword: '{}'", keyword);

        if (keyword == null || keyword.isBlank()) {
            return ApiResponse.error("Search keyword cannot be empty.");
        }

        try {
            ServiceResponse<PageResponseDTO<DiscussionInfoDTO>> serviceResponse =
                    discussionService.searchDiscussions(keyword, pageable);

            if (serviceResponse.isSuccess()) {
                return ApiResponse.success(serviceResponse.getDataObject(), "Search completed successfully.");
            } else {
                return ApiResponse.error(serviceResponse.getMessages(), "Failed to perform search.");
            }
        } catch (Exception e) {
            logger.error(String.format("Unexpected error while searching discussions for keyword '%s': ", keyword), e);
            return ApiResponse.error("An unexpected error occurred during the search.");
        }
    }
}