package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.domain.Registration;
import com.github.chipolaris.bootforum2.dto.ApiResponse;
import com.github.chipolaris.bootforum2.dto.MessageResponse;
import com.github.chipolaris.bootforum2.dto.RegistrationRequest;
import com.github.chipolaris.bootforum2.service.RegistrationService;
import com.github.chipolaris.bootforum2.service.ServiceResponse;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RegistrationController {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationController.class);

    @Resource
    private RegistrationService registrationService;

    @PostMapping("/public/register") // Place under /public as it doesn't require auth
    public ApiResponse<?> registerUser(@Valid @RequestBody RegistrationRequest registrationRequest) {
        try {
            ServiceResponse<Registration> serviceResponse = registrationService.processRegistrationRequest(registrationRequest);

            if(serviceResponse.getAckCode() == ServiceResponse.AckCodeType.FAILURE) {
                return ApiResponse.error(serviceResponse.getMessages(), "Registration failed");
            }
            return ApiResponse.success(serviceResponse.getDataObject().getRegistrationKey(), "User registered successfully");
        } catch (Exception e) {
            logger.error("Unexpected registration error", e);
            return ApiResponse.error(String.format("An unexpected error occurred during registration.", e.getMessage()));
        }
    }
}
