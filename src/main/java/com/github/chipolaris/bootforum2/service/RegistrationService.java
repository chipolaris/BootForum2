package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.domain.Person;
import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.domain.Registration;
import com.github.chipolaris.bootforum2.dto.RegistrationRequest;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    @Resource
    private GenericDAO genericDAO;

    @Resource
    private PasswordEncoder passwordEncoder;

    /**
     * Registers a new user.
     * @param registrationRequest DTO containing registration data.
     * @return The newly created User object.
     * @throws RuntimeException if username or email already exists.
     */
    @Transactional(readOnly=false)
    public ServiceResponse<Registration> processRegistrationRequest(RegistrationRequest registrationRequest) {
        logger.info("Attempting to register new user: {}", registrationRequest.username());

        ServiceResponse<Registration> response = new ServiceResponse<>();

        // Check if username exists
        if (genericDAO.entityExists(User.class, "username", registrationRequest.username())
                || genericDAO.entityExists(Registration.class, "username", registrationRequest.firstName())) {
            logger.warn("Registration failed: Username '{}' already exists.", registrationRequest.username());
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE);
            response.addMessage("Error: Username is already taken!");
        }

        // Check if email exists (assuming Person holds the email)
        if (genericDAO.entityExists(Person.class, "email", registrationRequest.email())) {
            logger.warn("Registration failed: Email '{}' already exists.", registrationRequest.email());
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE);
            response.addMessage("Error: Email is already in use!");
        }

        /*
        // Create new Person
        Person person = new Person();
        person.setFirstName(registrationRequest.firstName());
        person.setLastName(registrationRequest.lastName());
        person.setEmail(registrationRequest.email());
        // Set other Person fields as needed (e.g., registration date)
        genericDAO.persist(person); // Save Person first to get ID if User depends on it

        // Create new User
        User user = new User();
        user.setUsername(registrationRequest.username());
        user.setPassword(passwordEncoder.encode(registrationRequest.password()));
        user.setPerson(person); // Link User to Person
        user.setAccountStatus(AccountStatus.ACTIVE); // Enable user by default, or implement activation logic
        user.setUserRole(UserRole.USER); // Assign default roles if necessary (e.g., ROLE_USER)

        genericDAO.persist(user);
        logger.info("Successfully registered user: {}", user.getUsername());

        response.setDataObject(user);
        */

        if(response.getAckCode() != ServiceResponse.AckCodeType.FAILURE) {
            // Create new Registration
            Registration registration = new Registration();
            registration.setUsername(registrationRequest.username());
            registration.setPassword(passwordEncoder.encode(registrationRequest.password()));
            registration.setEmail(registrationRequest.email());
            registration.setRegistrationKey(UUID.randomUUID().toString());

            genericDAO.persist(registration);
            response.setDataObject(registration);
        }

        return response;
    }
}
