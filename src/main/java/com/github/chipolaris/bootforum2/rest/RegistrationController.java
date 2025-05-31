package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.domain.Registration;
import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.dto.ApiResponse;
import com.github.chipolaris.bootforum2.dto.RegistrationDTO;
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
            ServiceResponse<Registration> serviceResponse = registrationService.newRegistration(registrationDTO);

            if(serviceResponse.getAckCode() == ServiceResponse.AckCodeType.FAILURE) {
                return ApiResponse.error(serviceResponse.getMessages(), "Registration failed");
            }
            return ApiResponse.success(serviceResponse.getDataObject().getRegistrationKey(), "User registered successfully");
        } catch (Exception e) {
            logger.error("Unexpected registration error", e);
            return ApiResponse.error(String.format("An unexpected error occurred during registration.", e.getMessage()));
        }
    }

    @PostMapping("/public/confirm-registration-email")
    public ApiResponse<?> confirmRegistrationEmail(@RequestParam String registrationKey) {
        try {
            ServiceResponse<User> serviceResponse = registrationService.confirmRegistrationEmail(registrationKey);
            if(serviceResponse.getAckCode() == ServiceResponse.AckCodeType.FAILURE) {
                return ApiResponse.error(serviceResponse.getMessages(), "Registration email confirmation failed");
            }
            // Success! Return a map of {"username": username, "email": email}
            return ApiResponse.success(Map.of("username", serviceResponse.getDataObject().getUsername(),
                    "email", serviceResponse.getDataObject().getPerson().getEmail()),
                    "Registration email confirmed successfully");
        } catch (Exception e) {
            logger.error("Unexpected registration email confirmation error", e);
            return ApiResponse.error(String.format("An unexpected error occurred during registration.", e.getMessage()));
        }
    }
}
