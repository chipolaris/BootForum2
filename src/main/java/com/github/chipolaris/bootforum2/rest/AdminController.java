package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.ApiResponse;
import com.github.chipolaris.bootforum2.service.DataInitializationService; // <-- IMPORT
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
    private final DataInitializationService dataInitializationService; // <-- INJECT

    public AdminController(IndexingService indexingService,
                           DataInitializationService dataInitializationService) { // <-- ADD TO CONSTRUCTOR
        this.indexingService = indexingService;
        this.dataInitializationService = dataInitializationService; // <-- INITIALIZE
    }

    /**
     * NEW: Endpoint to trigger generation of simulated users.
     * This is a long-running process that will execute in the background.
     *
     * @param count The number of users to generate.
     * @return An ApiResponse confirming that the process has started.
     */
    @PostMapping("/data/generate-users")
    public ApiResponse<?> triggerUserGeneration(@RequestParam(defaultValue = "50") int count) {
        logger.info("Admin request received to generate {} simulated users", count);

        // Add some basic sanity checks for the input parameter
        if (count <= 0 || count > 5000) {
            return ApiResponse.error("User count must be between 1 and 5,000.");
        }

        dataInitializationService.generateSimulatedUsers(count);

        String message = String.format("%d simulated users are being generated in the background. " +
                "This may take a few moments. Check server logs for progress.", count);
        return ApiResponse.success(message);
    }

    /**
     * Endpoint to trigger generation of simulated data.
     * This is a long-running process that will execute in the background.
     *
     * @return An ApiResponse confirming that the process has started.
     */
    @PostMapping("/data/generate")
    public ApiResponse<?> triggerDataGeneration() {
        logger.info("Admin request received to generate simulated data");

        // Call the async service method. This call returns immediately.
        dataInitializationService.generateSimulatedData();

        String message = "Simulated data generation has been started in the background. " +
                "This may take several minutes. Check server logs for progress and completion status.";
        return ApiResponse.success(message);
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