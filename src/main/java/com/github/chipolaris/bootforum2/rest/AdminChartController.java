package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.ApiResponse;
import com.github.chipolaris.bootforum2.dto.admin.AdminChartDTO;
import com.github.chipolaris.bootforum2.service.AdminChartService;
import com.github.chipolaris.bootforum2.service.ServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/charts")
public class AdminChartController {

    private static final Logger logger = LoggerFactory.getLogger(AdminChartController.class);

    private final AdminChartService adminChartService;

    public AdminChartController(AdminChartService adminChartService) {
        this.adminChartService = adminChartService;
    }

    @GetMapping
    public ApiResponse<?> getChartData() {
        logger.info("Getting chart data");
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
}