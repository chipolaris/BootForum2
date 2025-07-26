package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.ApiResponse;
import com.github.chipolaris.bootforum2.service.IndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final IndexingService indexingService;

    public AdminController(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    /**
     * Endpoint to trigger a full re-index of the database.
     * This is a long-running process that will execute in the background.
     *
     * @param target The target to re-index. Can be "all", "Discussion", or "Comment".
     * @return An ApiResponse confirming that the process has started.
     */
    @PostMapping("/indexing/reindex")
    public ApiResponse<?> triggerReindex(@RequestParam(defaultValue = "all") String target) {

        logger.info("Admin request received to re-index target: '{}'", target);

        // Basic validation for user feedback
        List<String> validTargets = List.of("all", "discussion", "comment");
        if (!validTargets.contains(target.toLowerCase())) {
            return ApiResponse.error("Invalid target specified. Valid targets are: " + validTargets);
        }

        // Call the async service method. This call returns immediately.
        indexingService.reindex(target);

        String message = String.format("Re-indexing process for target '%s' has been started in the background. Check server logs for progress and completion status.", target);
        return ApiResponse.success(message);
    }
}