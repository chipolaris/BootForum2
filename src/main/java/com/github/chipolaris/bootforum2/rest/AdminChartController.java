package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.ApiResponse;
import com.github.chipolaris.bootforum2.dto.admin.AdminChartDTO;
import com.github.chipolaris.bootforum2.dto.admin.ChartDataDTO; // ADDED
import com.github.chipolaris.bootforum2.service.AdminChartService;
import com.github.chipolaris.bootforum2.service.ServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminChartController {

    private static final Logger logger = LoggerFactory.getLogger(AdminChartController.class);

    private final AdminChartService adminChartService;

    public AdminChartController(AdminChartService adminChartService) {
        this.adminChartService = adminChartService;
    }

    @GetMapping("/charts")
    public ApiResponse<?> getChartData() {
        logger.info("Getting initial chart data");
        try {
            ServiceResponse<AdminChartDTO> response = adminChartService.getChartData();

            if(response.isSuccess()) {
                return ApiResponse.success(response.getDataObject());
            }
            else {
                return ApiResponse.error(response.getMessages(), "Error getting chart data");
            }
        }
        catch(Exception e) {
            logger.error("Unexpected error getting chart data", e);
            return ApiResponse.error(String.format("An unexpected error occurred getting chart data: %s", e.getMessage()));
        }
    }

    @GetMapping("/charts/top-terms")
    public ApiResponse<?> getTopTermsChartData(
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(defaultValue = "all") String period) { // MODIFIED
        logger.info("Getting top terms chart data with limit {} and period {}", limit, period);
        try {
            ServiceResponse<ChartDataDTO> response = adminChartService.getTopTermsChartData(limit, period); // MODIFIED

            if(response.isSuccess()) {
                return ApiResponse.success(response.getDataObject());
            }
            else {
                return ApiResponse.error(response.getMessages(), "Error getting top terms chart data");
            }
        }
        catch(Exception e) {
            logger.error("Unexpected error getting top terms chart data", e);
            return ApiResponse.error(String.format("An unexpected error occurred getting top terms chart data: %s", e.getMessage()));
        }
    }
}