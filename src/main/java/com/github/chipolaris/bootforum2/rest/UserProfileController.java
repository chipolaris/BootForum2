package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.ApiResponse;
import com.github.chipolaris.bootforum2.dto.UserProfileDTO;
import com.github.chipolaris.bootforum2.service.ServiceResponse;
import com.github.chipolaris.bootforum2.service.UserProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/users")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/{username}/profile")
    public ApiResponse<?> getUserProfile(@PathVariable String username) {

        ServiceResponse<UserProfileDTO> serviceResponse = userProfileService.getUserProfile(username);

        if (serviceResponse.isSuccess()) {
            return ApiResponse.success(serviceResponse.getDataObject());
        } else {
            // Distinguish between not found and other errors if necessary
            if (serviceResponse.getMessages().stream().anyMatch(m -> m.contains("not found"))) {
                return ApiResponse.error(serviceResponse.getMessages(), "User not found");
            }
            return ApiResponse.error(serviceResponse.getMessages(), "Failed to retrieve user profile.");
        }
    }
}