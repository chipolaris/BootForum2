package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.domain.Forum;
import com.github.chipolaris.bootforum2.dto.ApiResponse;
import com.github.chipolaris.bootforum2.dto.ForumDTO;
import com.github.chipolaris.bootforum2.service.GenericService;
import com.github.chipolaris.bootforum2.service.ServiceResponse;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api")
public class ForumController {

    private static final Logger logger = LoggerFactory.getLogger(ForumController.class);

    @Resource
    private GenericService genericService;

    @PostMapping("/admin/create-forum")
    public ApiResponse<?> createForum(@Valid @RequestBody ForumDTO forumDTO) {
        try {
            Forum forum = forumDTO.newForum(); // get basic values from DTO
            ServiceResponse<Void> serviceResponse = genericService.saveEntity(forum);

            if(serviceResponse.getAckCode() == ServiceResponse.AckCodeType.FAILURE) {
                return ApiResponse.error(serviceResponse.getMessages(),"Forum creation failed");
            }
            return ApiResponse.success(ForumDTO.fromForum(forum),"Forum created successfully");
        } catch (Exception e) {
            logger.error("Unexpected create forum error", e);
            return ApiResponse.error(String.format("An unexpected error occurred during creating forum.", e.getMessage()));
        }
    }

    /**
     * Retrieves a specific forum by its ID.
     * This endpoint is assumed to be admin-protected.
     *
     * @param id The ID of the forum to retrieve.
     * @return ApiResponse containing the Forum or an error message.
     */
    @GetMapping("/admin/forums/{id}") // Path for retrieving a specific forum
    public ApiResponse<?> getForum(@PathVariable Long id) {
        try {
            // Assuming genericService.find returns the entity or null if not found
            Forum forum = genericService.findEntity(Forum.class, id).getDataObject();

            if (forum == null) {
                return ApiResponse.error(String.format("Forum with ID %d not found.", id));
            }

            return ApiResponse.success(ForumDTO.fromForum(forum), "Forum retrieved successfully");
        } catch (Exception e) {
            logger.error(String.format("Error retrieving forum with ID %d", id), e);
            return ApiResponse.error(String.format("An unexpected error occurred while retrieving forum: %s", e.getMessage()));
        }
    }

    /**
     * Retrieves a list of all forums.
     * This endpoint is assumed to be admin-protected.
     *
     * @return ApiResponse containing a list of Forums or an error message.
     */
    @GetMapping("/admin/forums") // Path for retrieving all forums
    public ApiResponse<?> getAllForums() {
        try {
            ServiceResponse<List<Forum>> response = genericService.getAllEntities(Forum.class);

            if (response.getAckCode() == ServiceResponse.AckCodeType.SUCCESS) {

                List<ForumDTO> forumDTOS = response.getDataObject().stream().map(ForumDTO::fromForum).toList();

                return ApiResponse.success(forumDTOS, "Forums retrieved successfully");
            } else {
                // Use messages from ServiceResponse if available
                return ApiResponse.error(response.getMessages(), "Fetch Error");
            }
        } catch (Exception e) {
            logger.error("Error retrieving all forums", e);
            return ApiResponse.error("An unexpected error occurred while retrieving forums: " + e.getMessage());
        }
    }

    /**
     * Updates an existing forum.
     * This endpoint is assumed to be admin-protected.
     *
     * @param id The ID of the forum to update.
     * @param forumDTO The request body containing updated forum data.
     * @return ApiResponse containing the updated Forum or an error message.
     */
    @PutMapping("/admin/forums/{id}") // Path for updating a specific forum
    public ApiResponse<?> updateForum(@PathVariable Long id, @Valid @RequestBody ForumDTO forumDTO) {

        try {
            Forum existingForum = genericService.findEntity(Forum.class, id).getDataObject();

            if (existingForum == null) {
                return ApiResponse.error(String.format("Forum with ID %d not found for update.", id));
            }

            // Update the properties of the existing forum entity from the request DTO
            existingForum.setTitle(forumDTO.title());
            existingForum.setDescription(forumDTO.description());
            existingForum.setIcon(forumDTO.icon());
            existingForum.setIconColor(forumDTO.iconColor());
            existingForum.setActive(forumDTO.active());
            // Note: Fields like createdBy, createDate are typically not updated.
            // updatedBy and updateDate might be handled by JPA @PreUpdate listeners or service logic.

            ServiceResponse<Void> serviceResponse = genericService.saveEntity(existingForum); // saveEntity should handle updates for managed entities with an ID

            if (serviceResponse.getAckCode() == ServiceResponse.AckCodeType.FAILURE) {
                return ApiResponse.error(serviceResponse.getMessages(), "Forum update failed");
            }

            // Return the updated forum entity.
            // The 'existingForum' instance is managed by JPA and reflects the saved state.
            return ApiResponse.success(existingForum, "Forum updated successfully");

        } catch (Exception e) {
            logger.error(String.format("Unexpected error updating forum with ID %d", id), e);
            return ApiResponse.error(String.format("An unexpected error occurred during updating forum: %s", e.getMessage()));
        }
    }


}
