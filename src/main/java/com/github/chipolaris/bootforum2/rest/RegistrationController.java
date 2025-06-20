package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.dto.ApiResponse;
import com.github.chipolaris.bootforum2.dto.RegistrationCreatedDTO;
import com.github.chipolaris.bootforum2.dto.RegistrationDTO;
import com.github.chipolaris.bootforum2.dto.UserRegisteredDTO;
import com.github.chipolaris.bootforum2.service.RegistrationService;
import com.github.chipolaris.bootforum2.service.ServiceResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class RegistrationController {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationController.class);

    @Autowired
    private RegistrationService registrationService;

    @PostMapping("/public/register") // Place under /public as it doesn't require auth
    public ApiResponse<?> registerUser(@Valid @RequestBody RegistrationDTO registrationDTO) {
        try {
            ServiceResponse<RegistrationCreatedDTO> serviceResponse = registrationService.newRegistration(registrationDTO);

            if(serviceResponse.isFailure()) {
                return ApiResponse.error(serviceResponse.getMessages(), "Registration failed");
            }
            return ApiResponse.success(serviceResponse.getDataObject().registrationKey(), "User registration submitted");
        } catch (Exception e) {
            logger.error("Unexpected registration error", e);
            return ApiResponse.error("An unexpected error occurred submitting registration: %s".formatted(e.getMessage()));
        }
    }

    @PostMapping("/public/confirm-registration-email")
    public ApiResponse<?> confirmRegistrationEmail(@RequestParam String registrationKey) {
        try {
            ServiceResponse<UserRegisteredDTO> serviceResponse = registrationService.confirmRegistration(registrationKey);
            if(serviceResponse.isFailure()) {
                return ApiResponse.error(serviceResponse.getMessages(), "Registration email confirmation failed");
            }
            // Success! Return a map of {"username": username, "email": email}
            return ApiResponse.success(Map.of("username", serviceResponse.getDataObject().username(),
                    "email", serviceResponse.getDataObject().email()),
                    "Registration email confirmed successfully");
        } catch (Exception e) {
            logger.error("Unexpected registration email confirmation error", e);
            return ApiResponse.error("An unexpected error occurred during registration: %s".formatted(e.getMessage()));
        }
    }
}
