package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.MessageResponse;
import com.github.chipolaris.bootforum2.dto.SignUpRequest;
import com.github.chipolaris.bootforum2.service.RegistrationService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public class RegistrationController {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationController.class);

    @Resource
    private RegistrationService registrationService;

    @PostMapping("/public/register") // Place under /public as it doesn't require auth
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        try {
            registrationService.registerNewUser(signUpRequest);
            return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
        } catch (RuntimeException e) {
            // Catch exceptions from the service (e.g., user/email exists)
            logger.error("Registration error: {}", e.getMessage());
            // Return a more specific status code like 409 Conflict if appropriate
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected registration error", e);
            return ResponseEntity.internalServerError().body(new MessageResponse("An unexpected error occurred during registration."));
        }
    }
}
