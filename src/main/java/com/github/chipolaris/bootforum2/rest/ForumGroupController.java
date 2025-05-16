package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dao.QueryMeta;
import com.github.chipolaris.bootforum2.domain.ForumGroup;
import com.github.chipolaris.bootforum2.dto.ApiResponse;
import com.github.chipolaris.bootforum2.dto.ForumGroupCreateDTO;
import com.github.chipolaris.bootforum2.dto.ForumGroupDTO;
import com.github.chipolaris.bootforum2.dto.ForumGroupUpdateDTO;
import com.github.chipolaris.bootforum2.service.ForumGroupService;
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
public class ForumGroupController {

    private static final Logger logger = LoggerFactory.getLogger(ForumGroupController.class);

    @Resource
    private GenericService genericService;

    @Resource
    private ForumGroupService forumGroupService;

    //@PostMapping("/admin/create-forum-group")
    public ApiResponse<?> createForumGroupOld(@Valid @RequestBody ForumGroupDTO forumGroupDTO) {
        try {
            ForumGroup forumGroup = forumGroupDTO.createForumGroup(); // get basic values from DTO
            ServiceResponse<Void> serviceResponse = genericService.saveEntity(forumGroup);

            if(serviceResponse.getAckCode() == ServiceResponse.AckCodeType.FAILURE) {
                return ApiResponse.error(serviceResponse.getMessages(),"Forum creation failed");
            }
            return ApiResponse.success(ForumGroupDTO.fromForumGroup(forumGroup),"ForumGroup created successfully");
        } catch (Exception e) {
            logger.error("Unexpected create forum group error", e);
            return ApiResponse.error(String.format("An unexpected error occurred during creating forum group.", e.getMessage()));
        }
    }

    @PostMapping("/admin/create-forum-group")
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
    // @GetMapping("/admin/forum-groups/{id}") // Path for retrieving a specific forum
    public ApiResponse<?> getForumGroupOld(@PathVariable Long id) {
        try {
            // Assuming genericService.find returns the entity or null if not found
            ForumGroup forumGroup = genericService.findEntity(ForumGroup.class, id).getDataObject();

            if (forumGroup == null) {
                return ApiResponse.error(String.format("Forum group with ID %d not found.", id));
            }

            return ApiResponse.success(ForumGroupDTO.fromForumGroup(forumGroup), "Forum group retrieved successfully");
        } catch (Exception e) {
            logger.error(String.format("Error retrieving forum group with ID %d", id), e);
            return ApiResponse.error(String.format("An unexpected error occurred while retrieving forum group: %s", e.getMessage()));
        }
    }

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
     * @param forumGroupDTO The request body containing updated forum data.
     * @return ApiResponse containing the updated Forum or an error message.
     */
    //@PutMapping("/admin/forum-groups/{id}") // Path for updating a specific forum
    public ApiResponse<?> updateForumGroupOld(@PathVariable Long id, @Valid @RequestBody ForumGroupDTO forumGroupDTO) {

        try {
            ForumGroup existingForumGroup = genericService.findEntity(ForumGroup.class, id).getDataObject();

            if (existingForumGroup == null) {
                return ApiResponse.error(String.format("Forum group with ID %d not found for update.", id));
            }

            // Update the properties of the existing forum entity from the request DTO
            existingForumGroup.setTitle(forumGroupDTO.title());
            existingForumGroup.setIcon(forumGroupDTO.icon());
            existingForumGroup.setIconColor(forumGroupDTO.iconColor());
            // Note: Fields like createdBy, createDate are typically not updated.
            // updatedBy and updateDate might be handled by JPA @PreUpdate listeners or service logic.

            ServiceResponse<Void> serviceResponse = genericService.saveEntity(existingForumGroup); // saveEntity should handle updates for managed entities with an ID

            if (serviceResponse.getAckCode() == ServiceResponse.AckCodeType.FAILURE) {
                return ApiResponse.error(serviceResponse.getMessages(), "Forum group update failed");
            }

            // Return the updated forum entity.
            // The 'existingForumGroup' instance is managed by JPA and reflects the saved state.
            return ApiResponse.success(ForumGroupDTO.fromForumGroup(existingForumGroup), "Forum group updated successfully");

        } catch (Exception e) {
            logger.error(String.format("Unexpected error updating forum group with ID %d", id), e);
            return ApiResponse.error(String.format("An unexpected error occurred during updating forum group: %s", e.getMessage()));
        }
    }

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
    //@GetMapping("/admin/root-forum-group") // Path for retrieving all forums
    public ApiResponse<?> getRootForumGroupOld() {
        try {
            QueryMeta<ForumGroup> queryMeta = QueryMeta.builder(ForumGroup.class).maxResult(1).filterMeta("parent", null, "EQUALS", "AND").build();
            ServiceResponse<List<ForumGroup>> response = genericService.findEntities(queryMeta);
            if (response.getAckCode() == ServiceResponse.AckCodeType.SUCCESS) {
                List<ForumGroup> forumGroups = response.getDataObject();
                if (!forumGroups.isEmpty()) {
                    ForumGroup rootForumGroup = forumGroups.get(0);
                    return ApiResponse.success(ForumGroupDTO.fromForumGroup(rootForumGroup), "Root forum group retrieved successfully");
                }
            }
            return ApiResponse.error("No root forum group found");
        } catch (Exception e) {
            logger.error("Error retrieving the root forum group", e);
            return ApiResponse.error("An unexpected error occurred while retrieving forum groups: " + e.getMessage());
        }
    }

    @GetMapping("/admin/root-forum-group") // Path for retrieving all forums
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
}
