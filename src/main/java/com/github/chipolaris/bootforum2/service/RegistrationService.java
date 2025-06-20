package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.Person;
import com.github.chipolaris.bootforum2.domain.Registration;
import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.dto.RegistrationCreatedDTO;
import com.github.chipolaris.bootforum2.dto.RegistrationDTO;
import com.github.chipolaris.bootforum2.dto.UserRegisteredDTO;
import com.github.chipolaris.bootforum2.event.UserCreatedEvent;
import com.github.chipolaris.bootforum2.mapper.RegistrationMapper;
import com.github.chipolaris.bootforum2.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    private final GenericDAO genericDAO;
    private final DynamicDAO dynamicDAO;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationMapper registrationMapper;
    private final ApplicationEventPublisher eventPublisher;

    // Note: in Spring version >= 4.3, @AutoWired is implied for beans with single constructor
    public RegistrationService(GenericDAO genericDAO, DynamicDAO dynamicDAO, UserMapper userMapper,
                               PasswordEncoder passwordEncoder, RegistrationMapper registrationMapper,
                               ApplicationEventPublisher eventPublisher) {
        this.genericDAO = genericDAO;
        this.dynamicDAO = dynamicDAO;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.registrationMapper = registrationMapper;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create a new Registration
     * @param registrationDTO DTO containing registration data.
     * @return The RegistrationCreatedDTO contains the newly created Registration's ID and registrationKey.
     * @throws RuntimeException if username or email already exists.
     */
    @Transactional(readOnly=false)
    public ServiceResponse<RegistrationCreatedDTO> newRegistration(RegistrationDTO registrationDTO) {
        logger.info("Attempting to register new user: {}", registrationDTO.username());

        ServiceResponse<RegistrationCreatedDTO> response = new ServiceResponse<>();

        // Check if username exists
        if(dynamicDAO.exists(QuerySpec.builder(User.class).filter(FilterSpec.eq("username", registrationDTO.username())).build())
            || dynamicDAO.exists(QuerySpec.builder(Registration.class).filter(FilterSpec.eq("username", registrationDTO.username())).build())) {
            logger.warn("Registration failed: Username '{}' already exists.", registrationDTO.username());
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE);
            response.addMessage("Error: Username is already taken!");
        }

        // Check if email exists (assuming Person holds the email)
        if(dynamicDAO.exists(QuerySpec.builder(Person.class).filter(FilterSpec.eq("email", registrationDTO.email().toLowerCase())).build())
                || dynamicDAO.exists(QuerySpec.builder(Registration.class).filter(FilterSpec.eq("email", registrationDTO.email().toLowerCase())).build())) {

            logger.warn("Registration failed: Email '{}' already exists.", registrationDTO.email());
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE);
            response.addMessage("Error: Email is already in use!");
        }

        if(!response.isFailure()) {
            // Use mapper to convert RegistrationDTO to Registration object
            Registration registration = registrationMapper.toEntity(registrationDTO);

            // Manually set fields not handled by the mapper. Also note the mapper set email to lower case
            registration.setPassword(passwordEncoder.encode(registrationDTO.password()));
            registration.setRegistrationKey(UUID.randomUUID().toString());

            genericDAO.persist(registration);
            response.setDataObject(registrationMapper.toRegistrationCreatedDTO(registration));
        }

        return response;
    }

    /**
     * Confirm a registration. If exists, create a new User. Afterward, delete the Registration.
     * @param registrationKey
     * @return
     */
    @Transactional(readOnly=false)
    public ServiceResponse<UserRegisteredDTO> confirmRegistration(String registrationKey) {

        Registration registration = dynamicDAO.<Registration>findOptional(QuerySpec.builder(Registration.class)
                .filter(FilterSpec.eq("registrationKey", registrationKey)).build()).orElse(null);

        if(registration == null) {
            logger.warn("Invalid registration key: {}", registrationKey);
            return ServiceResponse.failure("Invalid registration key: '%s'".formatted(registrationKey));
        }
        else {
            // Initialize user
            User user = initializeUser(registration);

            genericDAO.persist(user); // persist User object
            genericDAO.remove(registration); // cleanup the Registration object

            // Publish event
            eventPublisher.publishEvent(new UserCreatedEvent(this, user));

            logger.info("Successfully registered user: {}", user.getUsername());
            return ServiceResponse.success("User registered successfully", userMapper.toUserRegisteredDTO(user));
        }
    }

    /**
     * Helper method to create a User object out of a Registration.
     * @param registration
     * @return
     */
    private User initializeUser(Registration registration) {

        /*
         * The followings are created in User constructor:
         * Person, Preferences, UserStat, accountStatus = 'ACTIVE', userRole=UserRole.USER
         */
        User user = User.newUser();

        user.setUsername(registration.getUsername());
        // note: no need to encode password as it is already encoded in Registration
        user.setPassword(registration.getPassword());

        Person person = user.getPerson(); // person is created in User constructor
        person.setEmail(registration.getEmail());
        person.setFirstName(registration.getFirstName());
        person.setLastName(registration.getLastName());

        return user;
    }
}
