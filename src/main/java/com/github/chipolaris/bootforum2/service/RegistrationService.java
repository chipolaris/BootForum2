package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.domain.*;
import com.github.chipolaris.bootforum2.dto.RegistrationRequest;
import com.github.chipolaris.bootforum2.enumeration.AccountStatus;
import com.github.chipolaris.bootforum2.enumeration.UserRole;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    @Resource
    private GenericDAO genericDAO;

    @Resource
    private PasswordEncoder passwordEncoder;

    /**
     * Create a new Registration
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
        if (genericDAO.entityExists(Person.class, "email", registrationRequest.email())
                || genericDAO.entityExists(Registration.class, "email", registrationRequest.email())) {
            logger.warn("Registration failed: Email '{}' already exists.", registrationRequest.email());
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE);
            response.addMessage("Error: Email is already in use!");
        }

        if(response.getAckCode() != ServiceResponse.AckCodeType.FAILURE) {
            // Create new Registration
            Registration registration = new Registration();
            registration.setUsername(registrationRequest.username());
            registration.setPassword(passwordEncoder.encode(registrationRequest.password()));
            registration.setEmail(registrationRequest.email().toLowerCase()); // Ensure email is lowercase
            registration.setRegistrationKey(UUID.randomUUID().toString());

            genericDAO.persist(registration);
            response.setDataObject(registration);
        }

        return response;
    }

    /**
     * Confirm email for a registration. If exists, create a new User. Afterward, delete the Registration.
     * @param registrationKey
     * @return
     */
    @Transactional(readOnly=false)
    public ServiceResponse<User> processEmailConfirmation(String registrationKey) {
        ServiceResponse<User> response = new ServiceResponse<>();

        Registration registration = genericDAO.getEntity(Registration.class, Map.of("registrationKey", registrationKey));

        if(registration == null) {
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE);
            response.addMessage("Error: Invalid registration key");
        }
        else {
            // Initialize user
            User user = initializeUser(registration);

            genericDAO.persist(user); // persist User object
            genericDAO.remove(registration); // cleanup the Registration object
            logger.info("Successfully registered user: {}", user.getUsername());

            response.setDataObject(user);
        }

        return response;
    }

    /**
     * Helper method to create a User object out of a Registration.
     * @param registration
     * @return
     */
    private User initializeUser(Registration registration) {

        User user = new User();
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setUserRole(UserRole.USER);
        user.setPerson(new Person());
        user.setPreferences(new Preferences());
        user.setStat(new UserStat());

        user.setUsername(registration.getUsername());
        // note: no need to encode password as it is already encoded in Registration
        user.setPassword(registration.getPassword());
        user.getPerson().setEmail(registration.getEmail());

        return user;
    }
}
