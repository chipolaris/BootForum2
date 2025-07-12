package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.ApiResponse;
import com.github.chipolaris.bootforum2.dto.AvatarDTO;
import com.github.chipolaris.bootforum2.dto.FileResourceDTO;
import com.github.chipolaris.bootforum2.service.AvatarService;
import com.github.chipolaris.bootforum2.service.ServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class AvatarController {

    private static final Logger logger = LoggerFactory.getLogger(AvatarController.class);

    private final AvatarService avatarService;

    public AvatarController(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    @GetMapping("/user/avatar/my-avatar")
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
    @PostMapping(value = "/user/avatar/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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

    /**
     * Serves the avatar image for a given user.
     * If the user does not have a custom avatar, a default avatar is returned.
     * This endpoint is public and can be cached by browsers.
     *
     * @param username The username of the user.
     * @return A ResponseEntity containing the avatar image resource.
     */
    @Deprecated
    @GetMapping("/public/avatar/{username}")
    public ResponseEntity<Resource> getUserAvatar(@PathVariable String username) {

        // avatarService.getAvatarResource always return a success response with either
        // the user's avatar or a default avatar
        ServiceResponse<FileResourceDTO> serviceResponse = avatarService.getAvatarResource(username);

        // no need to check if serviceResponse is success or failure
        FileResourceDTO avatarResource = serviceResponse.getDataObject();
        Resource resource = avatarResource.resource();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatarResource.mimeType()))
                .cacheControl(CacheControl.maxAge(24, TimeUnit.HOURS).cachePublic())
                .body(resource);
    }

    /**
     * Return a user's avatar file id. or null if not found
     * @param username
     * @return
     */
    @GetMapping("/public/avatars/id/{username}")
    public ApiResponse<?> getAvatarFileId(@PathVariable String username) {

        // avatarService.getAvatarResource always return a success response with either
        // the user's avatar file id or a null value
        ServiceResponse<Long> serviceResponse = avatarService.getAvatarFileId(username);

        if(serviceResponse.isSuccess()) {
            Long avatarFileId = serviceResponse.getDataObject();
            return ApiResponse.success(avatarFileId, "Avatar file id retrieved successfully");
        }
        else { // this path is currently unreachable but provides future-proofing
            return ApiResponse.error(serviceResponse.getMessages(), "Failed to retrieve avatar file id");
        }
    }

    /**
     * Retrieves a map of usernames to their corresponding avatar file IDs.
     * This is useful for batch-fetching avatar information for multiple users at once.
     *
     * @param usernames A comma-separated list of usernames (e.g., ?usernames=user1,user2,user3).
     * @return An ApiResponse containing a map of usernames to avatar file IDs.
     */
    @GetMapping("/public/avatars/ids")
    public ApiResponse<?> getAvatarFileIds(@RequestParam("usernames") List<String> usernames) {

        ServiceResponse<Map<String, Long>> serviceResponse = avatarService.getAvatarFileIds(usernames);

        // The service method is designed to always succeed, but we check for robustness.
        if (serviceResponse.isSuccess()) {
            return ApiResponse.success(serviceResponse.getDataObject(), serviceResponse.getMessages().get(0));
        } else {
            // This path is currently unreachable but provides future-proofing.
            return ApiResponse.error(serviceResponse.getMessages(), "Failed to retrieve avatar file IDs");
        }
    }
}