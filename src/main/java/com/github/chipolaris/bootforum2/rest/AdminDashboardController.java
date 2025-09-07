package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.ApiResponse;
import com.github.chipolaris.bootforum2.dto.AdminDashboardDTO;
import com.github.chipolaris.bootforum2.service.AdminDashboardService;
import com.github.chipolaris.bootforum2.service.ServiceResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping
    public ApiResponse<?> getDashboardData(@RequestParam(defaultValue = "all") String timeWindow) {
        ServiceResponse<AdminDashboardDTO> response = adminDashboardService.getDashboardData(timeWindow);

        if (response.isSuccess()) {
            return ApiResponse.success(response.getDataObject());
        }

        return ApiResponse.error(response.getMessages(), "Failed to retrieve dashboard data.");
    }
}