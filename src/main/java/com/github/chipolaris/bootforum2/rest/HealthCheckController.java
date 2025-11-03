package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.ApiResponse;
import com.github.chipolaris.bootforum2.dto.ApplicationHealthDTO;
import com.github.chipolaris.bootforum2.dto.HealthStatusDTO;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/health")
public class HealthCheckController {

    private final HealthEndpoint healthEndpoint;

    public HealthCheckController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @GetMapping
    public ApiResponse<ApplicationHealthDTO> getApplicationHealth() {

        HealthComponent health = healthEndpoint.health();
        Status overallStatus = health.getStatus();

        // Check if the health component is a composite to get its sub-components
        Map<String, HealthComponent> components = Collections.emptyMap();
        if (health instanceof CompositeHealth compositeHealth) {
            components = compositeHealth.getComponents();
        }

        List<HealthStatusDTO> componentHealths = components.entrySet().stream()
                .map(entry -> {
                    HealthComponent componentValue = entry.getValue();
                    Map<String, Object> details = Collections.emptyMap();

                    // Check if the component is a Health instance to get its details
                    if (componentValue instanceof Health healthDetails) {
                        details = healthDetails.getDetails();
                    }

                    return new HealthStatusDTO(
                            entry.getKey(),
                            componentValue.getStatus().getCode(),
                            details);
                })
                .collect(Collectors.toList());

        ApplicationHealthDTO healthDTO = new ApplicationHealthDTO(overallStatus.getCode(), componentHealths);

        return ApiResponse.success(healthDTO, "Health status retrieved successfully.");
    }
}