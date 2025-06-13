package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.JwtAuthenticationResponse;
import com.github.chipolaris.bootforum2.dto.LoginRequest;
import com.github.chipolaris.bootforum2.event.UserLoginSuccessEvent; // Import the new event
import com.github.chipolaris.bootforum2.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher; // Import ApplicationEventPublisher
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final ApplicationEventPublisher eventPublisher; // Inject ApplicationEventPublisher

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider tokenProvider,
                          ApplicationEventPublisher eventPublisher) { // Add to constructor
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.eventPublisher = eventPublisher; // Initialize
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        logger.info(String.format("Authentication attempt for user: %s", loginRequest.username()));

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.username(),
                        loginRequest.password()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);

        // Publish the login success event
        try {
            eventPublisher.publishEvent(new UserLoginSuccessEvent(this, loginRequest.username()));
            logger.info(String.format("UserLoginSuccessEvent published for user: %s", loginRequest.username()));
        } catch (Exception e) {
            // Log the exception, but don't let it fail the login process
            logger.error(String.format("Error publishing UserLoginSuccessEvent for user %s: ", loginRequest.username()), e);
        }

        return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
    }
}