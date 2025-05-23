package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.domain.Person;
import com.github.chipolaris.bootforum2.domain.Registration;
import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.dto.RegistrationDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test") // Ensure you have a test profile configured (e.g., for H2 or Testcontainers)
@Transactional // Rolls back database changes after each test
class RegistrationServiceIntegrationTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder; // For verifying encoded passwords

    @BeforeEach
    void setUp() {
        // Clean up existing data to ensure a fresh state for each test
        entityManager.createQuery("DELETE FROM Comment").executeUpdate(); // If comments are linked and might cause FK issues
        entityManager.createQuery("DELETE FROM Discussion").executeUpdate(); // If discussions are linked
        entityManager.createQuery("DELETE FROM User").executeUpdate();
        entityManager.createQuery("DELETE FROM Person").executeUpdate();
        entityManager.createQuery("DELETE FROM Registration").executeUpdate();
        entityManager.flush();
    }

    // region newRegistration Tests
    @Test
    void newRegistration_whenUsernameAndEmailAreNew_shouldSucceedAndPersist() {
        // Arrange
        RegistrationDTO dto = new RegistrationDTO("newUser", "password123", "New", "User", "newuser@example.com");

        // Act
        ServiceResponse<Registration> response = registrationService.newRegistration(dto);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        assertNotNull(response.getDataObject());
        Long registrationId = response.getDataObject().getId();
        assertNotNull(registrationId);

        // Verify in DB
        Registration persistedRegistration = entityManager.find(Registration.class, registrationId);
        assertNotNull(persistedRegistration);
        assertEquals(dto.username(), persistedRegistration.getUsername());
        assertEquals(dto.email().toLowerCase(), persistedRegistration.getEmail());
        assertEquals(dto.firstName(), persistedRegistration.getFirstName());
        assertEquals(dto.lastName(), persistedRegistration.getLastName());
        assertTrue(passwordEncoder.matches(dto.password(), persistedRegistration.getPassword()));
        assertNotNull(persistedRegistration.getRegistrationKey());
        assertNotNull(persistedRegistration.getCreateDate());
        assertNotNull(persistedRegistration.getUpdateDate());
    }

    @Test
    void newRegistration_whenUsernameExistsInUserTable_shouldFail() {
        // Arrange
        User existingUser = new User();
        existingUser.setUsername("existingUser");
        existingUser.setPassword(passwordEncoder.encode("somepassword"));
        Person person = new Person();
        person.setEmail("unique-email-for-user@example.com");
        existingUser.setPerson(person); // Person is created in User constructor, but we set email
        entityManager.persist(person);
        entityManager.persist(existingUser);
        entityManager.flush();

        RegistrationDTO dto = new RegistrationDTO("existingUser", "password123", "Test", "User", "newemail@example.com");

        // Act
        ServiceResponse<Registration> response = registrationService.newRegistration(dto);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains("Error: Username is already taken!"));
    }

    @Test
    void newRegistration_whenUsernameExistsInRegistrationTable_shouldFail() {
        // Arrange
        Registration existingRegistration = new Registration();
        existingRegistration.setUsername("pendingUser");
        existingRegistration.setEmail("unique-email-for-reg@example.com");
        existingRegistration.setPassword(passwordEncoder.encode("somepassword"));
        existingRegistration.setRegistrationKey(UUID.randomUUID().toString());
        entityManager.persist(existingRegistration);
        entityManager.flush();

        RegistrationDTO dto = new RegistrationDTO("pendingUser", "password123", "Test", "User", "newemail@example.com");

        // Act
        ServiceResponse<Registration> response = registrationService.newRegistration(dto);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains("Error: Username is already taken!"));
    }

    @Test
    void newRegistration_whenEmailExistsInPersonTable_shouldFail() {
        // Arrange
        User existingUser = new User();
        existingUser.setUsername("anotherUser");
        existingUser.setPassword(passwordEncoder.encode("somepassword"));
        Person person = new Person();
        person.setEmail("existingemail@example.com"); // This email will cause the conflict
        existingUser.setPerson(person);
        entityManager.persist(person);
        entityManager.persist(existingUser);
        entityManager.flush();

        RegistrationDTO dto = new RegistrationDTO("newUser", "password123", "Test", "User", "existingemail@example.com");

        // Act
        ServiceResponse<Registration> response = registrationService.newRegistration(dto);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains("Error: Email is already in use!"));
    }

    @Test
    void newRegistration_whenEmailExistsInRegistrationTable_shouldFail() {
        // Arrange
        Registration existingRegistration = new Registration();
        existingRegistration.setUsername("somePendingUser");
        existingRegistration.setEmail("pendingemail@example.com"); // This email will cause the conflict
        existingRegistration.setPassword(passwordEncoder.encode("somepassword"));
        existingRegistration.setRegistrationKey(UUID.randomUUID().toString());
        entityManager.persist(existingRegistration);
        entityManager.flush();

        RegistrationDTO dto = new RegistrationDTO("newUser", "password123", "Test", "User", "pendingemail@example.com");

        // Act
        ServiceResponse<Registration> response = registrationService.newRegistration(dto);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains("Error: Email is already in use!"));
    }
    // endregion

    // region emailConfirmation Tests
    @Test
    void emailConfirmation_whenKeyIsValid_shouldSucceedCreateUserAndDeleteRegistration() {
        // Arrange
        String registrationKey = UUID.randomUUID().toString();
        Registration registration = new Registration();
        registration.setUsername("confirmUser");
        registration.setEmail("confirm@example.com");
        registration.setPassword(passwordEncoder.encode("password123"));
        registration.setFirstName("Confirm");
        registration.setLastName("User");
        registration.setRegistrationKey(registrationKey);
        entityManager.persist(registration);
        entityManager.flush();

        Long registrationId = registration.getId();

        // Act
        ServiceResponse<User> response = registrationService.emailConfirmation(registrationKey);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        assertNotNull(response.getDataObject());
        User createdUser = response.getDataObject();
        assertNotNull(createdUser.getId());

        // Verify User in DB
        User persistedUser = entityManager.find(User.class, createdUser.getId());
        assertNotNull(persistedUser);
        assertEquals("confirmUser", persistedUser.getUsername());
        assertEquals("confirm@example.com", persistedUser.getPerson().getEmail());
        assertEquals("Confirm", persistedUser.getPerson().getFirstName());
        assertEquals("User", persistedUser.getPerson().getLastName());
        assertTrue(passwordEncoder.matches("password123", persistedUser.getPassword()));

        // Verify Registration was deleted
        Registration deletedRegistration = entityManager.find(Registration.class, registrationId);
        assertNull(deletedRegistration);
    }

    @Test
    void emailConfirmation_whenKeyIsInvalid_shouldFail() {
        // Arrange
        String invalidKey = "this-key-does-not-exist";

        // Act
        ServiceResponse<User> response = registrationService.emailConfirmation(invalidKey);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains("Error: Invalid registration key"));

        // Verify no user was created
        Long userCount = entityManager.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult();
        assertEquals(0, userCount);
    }

    @Test
    void emailConfirmation_whenKeyIsValidButUserAlreadyExists_shouldStillCreateUserAndRemoveRegistration() {
        // This scenario tests if the service correctly handles the registration removal
        // even if, hypothetically, a user with the same username was created by another process
        // between registration and confirmation. The current service logic doesn't explicitly
        // re-check for user existence before creating, which is fine for this test's scope.
        // The primary goal here is to ensure the registration is cleaned up.

        // Arrange
        String registrationKey = UUID.randomUUID().toString();
        Registration registration = new Registration();
        registration.setUsername("existingConfirmUser");
        registration.setEmail("existingconfirm@example.com");
        registration.setPassword(passwordEncoder.encode("password123"));
        registration.setFirstName("ExistingConfirm");
        registration.setLastName("User");
        registration.setRegistrationKey(registrationKey);
        entityManager.persist(registration);
        entityManager.flush();
        Long registrationId = registration.getId();

        // Simulate a user already existing (though this shouldn't happen with proper unique constraints)
        User preExistingUser = new User();
        preExistingUser.setUsername("existingConfirmUser");
        preExistingUser.setPassword("someOtherPassword");
        Person person = new Person();
        person.setEmail("someotheremail@example.com"); // Different email to avoid unique constraint issues if any
        preExistingUser.setPerson(person);
        entityManager.persist(person);
        entityManager.persist(preExistingUser);
        entityManager.flush();


        // Act
        ServiceResponse<User> response = registrationService.emailConfirmation(registrationKey);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        assertNotNull(response.getDataObject());
        User createdUser = response.getDataObject();
        assertNotNull(createdUser.getId());
        assertNotEquals(preExistingUser.getId(), createdUser.getId(), "A new user should be created from registration data");

        // Verify User in DB (the one created from registration)
        User persistedUser = entityManager.find(User.class, createdUser.getId());
        assertNotNull(persistedUser);
        assertEquals("existingConfirmUser", persistedUser.getUsername());
        assertEquals("existingconfirm@example.com", persistedUser.getPerson().getEmail());

        // Verify Registration was deleted
        Registration deletedRegistration = entityManager.find(Registration.class, registrationId);
        assertNull(deletedRegistration);
    }

    // endregion
}