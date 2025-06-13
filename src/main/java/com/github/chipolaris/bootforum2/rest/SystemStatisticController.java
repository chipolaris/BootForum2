package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.SystemStatisticDTO;
import com.github.chipolaris.bootforum2.service.SystemStatistic;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/public")
public class SystemStatisticController {

    private final SystemStatistic systemStatistic;

    public SystemStatisticController(SystemStatistic systemStatistic) {
        this.systemStatistic = systemStatistic;
    }

    @RequestMapping("/system-statistic")
    public ResponseEntity<SystemStatisticDTO> getSystemStatistic() {
        return ResponseEntity.ok(systemStatistic.getDTO());
    }
}
