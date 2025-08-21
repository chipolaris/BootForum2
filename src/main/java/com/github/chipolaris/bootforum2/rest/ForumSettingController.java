package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.ApiResponse;
import com.github.chipolaris.bootforum2.dto.SettingDTO;
import com.github.chipolaris.bootforum2.service.ForumSettingService;
import com.github.chipolaris.bootforum2.service.ServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class ForumSettingController {

    private static final Logger logger = LoggerFactory.getLogger(ForumSettingController.class);

    private final ForumSettingService forumSettingService;

    public ForumSettingController(ForumSettingService forumSettingService) {
        this.forumSettingService = forumSettingService;
    }

    @GetMapping("/forum-settings/get")
    public ApiResponse<?> getForumSettings() {

        logger.info("Admin request received to fetch forum settings");

        try {
            ServiceResponse<Map<String, List<SettingDTO>>> serviceResponse =
                    forumSettingService.getAllSettings();

            if (serviceResponse.isFailure()) {
                return ApiResponse.error(serviceResponse.getMessages(), "Fetch Error");
            }
            else {
                return ApiResponse.success(serviceResponse.getDataObject(), "Settings fetched successfully");
            }
        } catch (Exception e) {
            logger.error("Error fetching forum settings", e);
            return ApiResponse.error("An unexpected error occurred while fetching forum settings: " + e.getMessage());
        }
    }

    @PostMapping("/forum-settings/update")
    public ApiResponse<?> updateForumSettings(@RequestBody Map<String, List<SettingDTO>> settings) {

        logger.info("Admin request received to update forum settings");

        try {
            ServiceResponse<Void> serviceResponse =
                    forumSettingService.saveOrUpdate(settings);

            if (serviceResponse.isFailure()) {
                return ApiResponse.error(serviceResponse.getMessages(), "Update Error");
            }
            else {
                return ApiResponse.success(null, "Settings updated successfully");
            }
        } catch (Exception e) {
            logger.error("Error updating forum settings", e);
            return ApiResponse.error("An unexpected error occurred while updating forum settings: " + e.getMessage());
        }
    }
}
