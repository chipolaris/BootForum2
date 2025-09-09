package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.ApiResponse;
import com.github.chipolaris.bootforum2.dto.AdminPasswordChangeDTO;
import com.github.chipolaris.bootforum2.dto.AdminUserUpdateDTO;
import com.github.chipolaris.bootforum2.dto.UserSummaryDTO;
import com.github.chipolaris.bootforum2.service.ServiceResponse;
import com.github.chipolaris.bootforum2.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserController.class);

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<?> getUsers(Pageable pageable) {
        try {
            ServiceResponse<Page<UserSummaryDTO>> response = userService.getUsers(pageable);

            if(response.isSuccess()) {
                return ApiResponse.success(response.getDataObject());
            }
            else {
                return ApiResponse.error(response.getMessages(), "Error getting users");
            }
        }
        catch(Exception e) {
            logger.error("Unexpected error getting users", e);
            return ApiResponse.error(String.format("An unexpected error occurred getting users: %s", e.getMessage()));
        }
    }

    @PutMapping("/{userId}")
    public ApiResponse<?> updateUser(@PathVariable Long userId, @RequestBody AdminUserUpdateDTO updateDTO) {
        try {
            ServiceResponse<Void> response = userService.updateUserByAdmin(userId, updateDTO);
            if(response.isSuccess()) {
                return ApiResponse.success("User updated successfully: " + userId);
            }
            else {
                return ApiResponse.error(response.getMessages(), "Error updating user: " + userId);
            }
        }
        catch(Exception e) {
            logger.error(String.format("Unexpected error updating user with ID %d", userId), e);
            return ApiResponse.error(String.format("An unexpected error occurred updating user: %s", e.getMessage()));
        }

    }

    @PostMapping("/{userId}/password")
    public ApiResponse<?> changePassword(@PathVariable Long userId, @RequestBody AdminPasswordChangeDTO passwordDTO) {
        try {
            ServiceResponse<Void> response = userService.changePasswordByAdmin(userId, passwordDTO);
            if(response.isSuccess()) {
                return ApiResponse.success("Password changed successfully for user: " + userId);
            }
            else {
                return ApiResponse.error(response.getMessages(), "Error changing password");
            }
        }
        catch(Exception e) {
            logger.error(String.format("Unexpected error changing password for user with ID %d", userId), e);
            return ApiResponse.error(String.format("An unexpected error occurred changing password: %s", e.getMessage()));
        }
    }
}