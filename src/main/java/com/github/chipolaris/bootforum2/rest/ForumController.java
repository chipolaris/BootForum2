package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.*;
import com.github.chipolaris.bootforum2.service.ForumService;
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
public class ForumController {

    private static final Logger logger = LoggerFactory.getLogger(ForumController.class);

    @Autowired
    private ForumService forumService;

    @PostMapping("/admin/forums/create")
    public ApiResponse<?> createForum(@Valid @RequestBody ForumCreateDTO forumCreateDTO) {
        try {

            ServiceResponse<ForumDTO> serviceResponse = forumService.createForum(forumCreateDTO);

            if(serviceResponse.getAckCode() == ServiceResponse.AckCodeType.FAILURE) {
                return ApiResponse.error(serviceResponse.getMessages(),"Forum creation failed");
            }
            return ApiResponse.success(serviceResponse.getDataObject(),"Forum created successfully");
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
    @GetMapping("/public/forums/{id}") // Path for retrieving a specific forum
    public ApiResponse<?> getForum(@PathVariable Long id) {
        try {
            ServiceResponse<ForumDTO> serviceResponse = forumService.getForum(id);

            if (serviceResponse.getAckCode() == ServiceResponse.AckCodeType.FAILURE) {
                return ApiResponse.error(String.format("Forum with ID %d not found.", id));
            }
            else {
                return ApiResponse.success(serviceResponse.getDataObject(), "Forum retrieved successfully");
            }
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
    @GetMapping("/admin/forums/all")
    public ApiResponse<?> getAllForums() {
        try {
            ServiceResponse<List<ForumDTO>> response = forumService.getAllForums();

            if (response.getAckCode() == ServiceResponse.AckCodeType.SUCCESS) {
                return ApiResponse.success(response.getDataObject(), "Forums retrieved successfully");
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
     * @param forumUpdateDTO The request body containing updated forum data.
     * @return ApiResponse containing the updated Forum or an error message.
     */
    @PutMapping("/admin/forums/{id}") // Path for updating a specific forum
    public ApiResponse<?> updateForum(@PathVariable Long id, @Valid @RequestBody ForumUpdateDTO forumUpdateDTO) {

        if(!Objects.equals(id, forumUpdateDTO.id())) {
            return ApiResponse.error(List.of("Forum ID mismatch"), "Forum update failed");
        }

        try {
            ServiceResponse<ForumDTO> serviceResponse = forumService.updateForum(forumUpdateDTO);

            if(serviceResponse.getAckCode() == ServiceResponse.AckCodeType.FAILURE) {
                return ApiResponse.error(serviceResponse.getMessages(), "Forum update failed");
            }
            else {
                return ApiResponse.success(serviceResponse.getDataObject(), "Forum updated successfully");
            }
        }
        catch (Exception e) {
            logger.error(String.format("Unexpected error updating forum with ID %d", id), e);
            return ApiResponse.error(String.format("An unexpected error occurred during updating forum: %s", e.getMessage()));
        }
    }
}
