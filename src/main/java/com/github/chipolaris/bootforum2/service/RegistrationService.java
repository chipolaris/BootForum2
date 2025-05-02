package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.domain.Person;
import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.dto.SignUpRequest;
import com.github.chipolaris.bootforum2.enumeration.AccountStatus;
import com.github.chipolaris.bootforum2.enumeration.UserRole;
import com.github.chipolaris.bootforum2.repository.PersonRepository;
import com.github.chipolaris.bootforum2.repository.UserRepository;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    @Resource
    private UserRepository userRepository;

    @Resource
    private PersonRepository personRepository;

    @Resource
    private PasswordEncoder passwordEncoder;

    /**
     * Registers a new user.
     * @param signUpRequest DTO containing registration data.
     * @return The newly created User object.
     * @throws RuntimeException if username or email already exists.
     */
    public User registerNewUser(SignUpRequest signUpRequest) {
        logger.info("Attempting to register new user: {}", signUpRequest.username());

        // Check if username exists
        if (userRepository.existsByUsername(signUpRequest.username())) {
            logger.warn("Registration failed: Username '{}' already exists.", signUpRequest.username());
            throw new RuntimeException("Error: Username is already taken!"); // Or a custom exception
        }

        // Check if email exists (assuming Person holds the email)
        if (personRepository.existsByEmail(signUpRequest.email())) {
            logger.warn("Registration failed: Email '{}' already exists.", signUpRequest.email());
            throw new RuntimeException("Error: Email is already in use!"); // Or a custom exception
        }

        // Create new Person
        Person person = new Person();
        person.setFirstName(signUpRequest.firstName());
        person.setLastName(signUpRequest.lastName());
        person.setEmail(signUpRequest.email());
        // Set other Person fields as needed (e.g., registration date)
        personRepository.save(person); // Save Person first to get ID if User depends on it

        // Create new User
        User user = new User();
        user.setUsername(signUpRequest.username());
        user.setPassword(passwordEncoder.encode(signUpRequest.password()));
        user.setPerson(person); // Link User to Person
        user.setAccountStatus(AccountStatus.ACTIVE); // Enable user by default, or implement activation logic
        user.setUserRole(UserRole.USER); // Assign default roles if necessary (e.g., ROLE_USER)

        User savedUser = userRepository.save(user);
        logger.info("Successfully registered user: {}", savedUser.getUsername());
        return savedUser;
    }
}
