package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.ApiResponse;
import com.github.chipolaris.bootforum2.service.ForumSettingService;
import com.github.chipolaris.bootforum2.service.ServiceResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/config")
public class PublicConfigController {

    private final ForumSettingService forumSettingService;

    public PublicConfigController(ForumSettingService forumSettingService) {
        this.forumSettingService = forumSettingService;
    }

    @PostMapping("/settings")
    public ApiResponse<?> getSettings(@RequestBody List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return ApiResponse.error("No setting keys provided.");
        }

        ServiceResponse<Map<String, Object>> response = forumSettingService.getSettingValues(keys);

        // This endpoint always returns success with the data it found.
        // The frontend is responsible for handling any missing keys.
        return ApiResponse.success(response.getDataObject());
    }

    @GetMapping("/setting")
    public ApiResponse<?> getSetting(@RequestParam String key) {
        // key is expected in "category.key" format, e.g., "general.siteDescription"
        String[] parts = key.split("\\.", 2);
        if (parts.length != 2) {
            return ApiResponse.error("Invalid setting key format. Expected 'category.key'.");
        }

        ServiceResponse<Object> response = forumSettingService.getSettingValue(parts[0], parts[1]);

        if (response.isSuccess()) {
            return ApiResponse.success(response.getDataObject());
        } else {
            // Return success with a null value to avoid breaking the frontend if a setting is not found
            return ApiResponse.success(null, response.getMessages().get(0));
        }
    }
}