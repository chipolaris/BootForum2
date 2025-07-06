package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.ApiResponse;
import com.github.chipolaris.bootforum2.dto.AvatarDTO;
import com.github.chipolaris.bootforum2.service.AvatarService;
import com.github.chipolaris.bootforum2.service.ServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user/avatar")
public class AvatarController {

    private static final Logger logger = LoggerFactory.getLogger(AvatarController.class);

    private final AvatarService avatarService;

    public AvatarController(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    @GetMapping("/my-avatar")
    public ApiResponse<?> getMyAvatar() {

        logger.info("Received request to get avatar.");

        ServiceResponse<AvatarDTO> serviceResponse = avatarService.getAvatar();

        if (serviceResponse.isSuccess()) {
            return ApiResponse.success(serviceResponse.getDataObject(), "Avatar retrieved successfully.");
        } else {
            return ApiResponse.error("Failed to retrieve avatar.");
        }
    }

    /**
     * Handles the upload of a user's avatar file.
     * This endpoint is protected and requires an authenticated user.
     *
     * @param avatarFile The avatar file sent as multipart/form-data.
     * @return A ResponseEntity containing an ApiResponse with the AvatarDTO or an error.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> uploadAvatar(@RequestParam("avatarFile") MultipartFile avatarFile) {

        logger.info("Received request to upload avatar. File name: {}", avatarFile.getOriginalFilename());

        if (avatarFile.isEmpty()) {
            return ApiResponse.error("Avatar file must be provided.");
        }

        try {
            ServiceResponse<AvatarDTO> serviceResponse = avatarService.uploadAvatar(avatarFile);

            if (serviceResponse.isSuccess()) {
                return ApiResponse.success(serviceResponse.getDataObject(), "Avatar uploaded successfully.");
            } else {
                // Return a 400 Bad Request for logical failures (e.g., user not authenticated)
                return ApiResponse.error(serviceResponse.getMessages(), "Failed to upload avatar");
            }
        } catch (Exception e) {
            logger.error("Unexpected error during avatar upload", e);

            return ApiResponse.error("An unexpected server error occurred. %s".formatted(e.getMessage()));
        }
    }
}