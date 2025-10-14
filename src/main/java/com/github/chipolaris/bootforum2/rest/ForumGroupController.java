package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.*;
import com.github.chipolaris.bootforum2.service.ForumGroupService;
import com.github.chipolaris.bootforum2.service.ServiceResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api")
public class ForumGroupController {

    private static final Logger logger = LoggerFactory.getLogger(ForumGroupController.class);

    @Autowired
    private ForumGroupService forumGroupService;

    @PostMapping("/admin/forum-groups")
    public ApiResponse<?> createForumGroup(@Valid @RequestBody ForumGroupCreateDTO forumGroupCreateDTO) {
        try {
            ServiceResponse<ForumGroupDTO> serviceResponse = forumGroupService.createForumGroup(forumGroupCreateDTO);

            if(serviceResponse.getAckCode() == ServiceResponse.AckCodeType.FAILURE) {
                return ApiResponse.error(serviceResponse.getMessages(),"Forum creation failed");
            }
            else {
                return ApiResponse.success(serviceResponse.getDataObject(), "ForumGroup created successfully");
            }
        } catch (Exception e) {
            logger.error("Unexpected create forum group error", e);
            return ApiResponse.error(String.format("An unexpected error occurred during creating forum group.", e.getMessage()));
        }
    }

    /**
     * Retrieves a specific forum by its ID.
     * This endpoint is assumed to be admin-protected.
     *
     * @param id The ID of the forum to retrieve.
     * @return ApiResponse containing the Forum or an error message.
     */
    @GetMapping("/admin/forum-groups/{id}") // Path for retrieving a specific forum
    public ApiResponse<?> getForumGroup(@PathVariable Long id) {
        try {

            ServiceResponse<ForumGroupDTO> serviceResponse = forumGroupService.getForumGroup(id);

            if (serviceResponse.getAckCode() == ServiceResponse.AckCodeType.FAILURE) {
                return ApiResponse.error(String.format("Forum group with ID %d not found.", id));
            }
            else {
                return ApiResponse.success(serviceResponse.getDataObject(), "Forum group retrieved successfully");
            }
        } catch (Exception e) {
            logger.error(String.format("Error retrieving forum group with ID %d", id), e);
            return ApiResponse.error(String.format("An unexpected error occurred while retrieving forum group: %s", e.getMessage()));
        }
    }

    /**
     * Updates an existing forum.
     * This endpoint is assumed to be admin-protected.
     *
     * @param id The ID of the forum to update.
     * @param forumGroupUpdateDTO The request body containing updated forum data.
     * @return ApiResponse containing the updated Forum or an error message.
     */
    @PutMapping("/admin/forum-groups/{id}") // Path for updating a specific forum
    public ApiResponse<?> updateForumGroup(@PathVariable Long id, @Valid @RequestBody ForumGroupUpdateDTO forumGroupUpdateDTO) {

        if(!Objects.equals(id, forumGroupUpdateDTO.id())) {
            return ApiResponse.error(List.of("Forum Group ID mismatch"), "Forum group update failed");
        }

        try {
            ServiceResponse<ForumGroupDTO> serviceResponse = forumGroupService.updateForumGroup(forumGroupUpdateDTO);

            if (serviceResponse.getAckCode() == ServiceResponse.AckCodeType.FAILURE) {
                return ApiResponse.error(serviceResponse.getMessages(), "Forum group update failed");
            }
            // Return the updated forum entity.
            return ApiResponse.success(serviceResponse.getDataObject(), "Forum group updated successfully");

        } catch (Exception e) {
            logger.error(String.format("Unexpected error updating forum group with ID %d", id), e);
            return ApiResponse.error(String.format("An unexpected error occurred during updating forum group: %s", e.getMessage()));
        }
    }

    /**
     * Retrieve the root forum group. The root forum group is the one with parent == null
     * This endpoint is assumed to be admin-protected.
     *
     * @return ApiResponse containing a list of Forums or an error message.
     */
    @GetMapping("/admin/forum-groups/root") // Path for retrieving all forums
    public ApiResponse<?> getRootForumGroup() {
        try {
            ServiceResponse<ForumGroupDTO> serviceResponse = forumGroupService.getRootForumGroup();

            if(serviceResponse.getAckCode() == ServiceResponse.AckCodeType.FAILURE) {
                return ApiResponse.error("No root forum group found");
            }
            else {
                return ApiResponse.success(serviceResponse.getDataObject(), "Root forum group retrieved successfully");
            }

        } catch (Exception e) {
            logger.error("Error retrieving the root forum group", e);
            return ApiResponse.error("An unexpected error occurred while retrieving forum groups: " + e.getMessage());
        }
    }

    @GetMapping("/public/forum-tree-table") // Path for retrieving all forums and forum groups
    public ApiResponse<?> getForumTreeTable() {
        try {
            ServiceResponse<ForumTreeTableDTO> serviceResponse = forumGroupService.getForumTreeTable();

            if(serviceResponse.getAckCode() == ServiceResponse.AckCodeType.FAILURE) {
                return ApiResponse.error("Error retrieving forum tree table");
            }
            else {
                return ApiResponse.success(serviceResponse.getDataObject(), "Forum tree table retrieved successfully");
            }

        } catch (Exception e) {
            logger.error("Error retrieving forum tree table", e);
            return ApiResponse.error("An unexpected error occurred while retrieving forum tree table: " + e.getMessage());
        }
    }
}
