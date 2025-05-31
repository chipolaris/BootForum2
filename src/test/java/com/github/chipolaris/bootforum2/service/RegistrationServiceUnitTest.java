package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.Person;
import com.github.chipolaris.bootforum2.domain.Registration;
import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.dto.RegistrationDTO;
import com.github.chipolaris.bootforum2.event.UserCreatedEvent;
import com.github.chipolaris.bootforum2.mapper.RegistrationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceUnitTest {

    @Mock
    private GenericDAO genericDAO;

    @Mock
    private DynamicDAO dynamicDAO;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RegistrationMapper registrationMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void newRegistration_whenUsernameAndEmailAreNew_shouldSucceed() {
        // Arrange
        RegistrationDTO dto = new RegistrationDTO("newUser", "password123", "New", "User", "newuser@example.com");
        Registration mappedRegistration = new Registration(); // Simulate what mapper returns
        mappedRegistration.setUsername(dto.username());
        mappedRegistration.setEmail(dto.email().toLowerCase()); // Mapper handles this
        mappedRegistration.setFirstName(dto.firstName());
        mappedRegistration.setLastName(dto.lastName());

        String encodedPassword = "encodedPassword";
        String registrationKeyRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

        when(dynamicDAO.exists(any(QuerySpec.class))).thenReturn(false); // No existing user/email
        when(registrationMapper.toEntity(dto)).thenReturn(mappedRegistration);
        when(passwordEncoder.encode(dto.password())).thenReturn(encodedPassword);
        // doNothing().when(genericDAO).persist(any(Registration.class)); // Not strictly needed if capturing

        // Act
        ServiceResponse<Registration> response = registrationService.newRegistration(dto);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        assertNotNull(response.getDataObject());

        ArgumentCaptor<Registration> registrationCaptor = ArgumentCaptor.forClass(Registration.class);
        verify(genericDAO).persist(registrationCaptor.capture());
        Registration persistedRegistration = registrationCaptor.getValue();

        assertEquals(dto.username(), persistedRegistration.getUsername());
        assertEquals(dto.email().toLowerCase(), persistedRegistration.getEmail());
        assertEquals(dto.firstName(), persistedRegistration.getFirstName());
        assertEquals(dto.lastName(), persistedRegistration.getLastName());
        assertEquals(encodedPassword, persistedRegistration.getPassword());
        assertNotNull(persistedRegistration.getRegistrationKey());
        assertTrue(persistedRegistration.getRegistrationKey().matches(registrationKeyRegex));

        verify(registrationMapper).toEntity(dto);
        verify(passwordEncoder).encode(dto.password());
        // Verify dynamicDAO.exists was called 4 times (2 for username, 2 for email)
        verify(dynamicDAO, times(4)).exists(any(QuerySpec.class));
    }

    @Test
    void newRegistration_whenUsernameExistsInUserTable_shouldFail() {
        // Arrange
        RegistrationDTO dto = new RegistrationDTO("existingUser", "password123", "Test", "User", "test@example.com");

        // Mock username exists in User table
        when(dynamicDAO.exists(argThat(qs -> qs != null && qs.getRootEntity().equals(User.class)
                && qs.getFilters() != null && !qs.getFilters().isEmpty()
                && qs.getFilters().get(0).field().equals("username")
                && qs.getFilters().get(0).value().equals(dto.username())))) // Be more specific with value
                .thenReturn(true); // Username exists in User

        // Email checks are still needed as the service evaluates that block regardless
        String lowerCaseEmail = dto.email().toLowerCase();
        when(dynamicDAO.exists(argThat(qs -> qs != null && qs.getRootEntity().equals(Person.class)
                && qs.getFilters() != null && !qs.getFilters().isEmpty()
                && qs.getFilters().get(0).field().equals("email")
                && qs.getFilters().get(0).value().equals(lowerCaseEmail))))
                .thenReturn(false); // Email is new in Person

        when(dynamicDAO.exists(argThat(qs -> qs != null && qs.getRootEntity().equals(Registration.class)
                && qs.getFilters() != null && !qs.getFilters().isEmpty()
                && qs.getFilters().get(0).field().equals("email")
                && qs.getFilters().get(0).value().equals(lowerCaseEmail))))
                .thenReturn(false); // Email is new in Registration

        // Act
        ServiceResponse<Registration> response = registrationService.newRegistration(dto);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains("Error: Username is already taken!"));
        verify(genericDAO, never()).persist(any());

        // Verify that the specific calls were made
        verify(dynamicDAO).exists(argThat(qs -> qs.getRootEntity().equals(User.class) && qs.getFilters().get(0).field().equals("username")));
        // The call to check username in Registration table should NOT have been made
        verify(dynamicDAO, never()).exists(argThat(qs -> qs.getRootEntity().equals(Registration.class) && qs.getFilters().get(0).field().equals("username")));
        // Verify email checks were made
        verify(dynamicDAO, times(1)).exists(argThat(qs -> qs.getRootEntity().equals(Person.class) && qs.getFilters().get(0).field().equals("email")));
        verify(dynamicDAO, times(1)).exists(argThat(qs -> qs.getRootEntity().equals(Registration.class) && qs.getFilters().get(0).field().equals("email")));
    }

    @Test
    void newRegistration_whenUsernameExistsInRegistrationTable_shouldFail() {
        // Arrange
        RegistrationDTO dto = new RegistrationDTO("pendingUser", "password123", "Test", "User", "test@example.com");
        String lowerCaseEmail = dto.email().toLowerCase();

        // Mock username does not exist in User table
        when(dynamicDAO.exists(argThat(qs -> qs != null && User.class.equals(qs.getRootEntity())
                && qs.getFilters() != null && !qs.getFilters().isEmpty()
                && "username".equals(qs.getFilters().get(0).field())
                && dto.username().equals(qs.getFilters().get(0).value()))))
                .thenReturn(false);

        // Mock username exists in Registration table
        when(dynamicDAO.exists(argThat(qs -> qs != null && Registration.class.equals(qs.getRootEntity())
                && qs.getFilters() != null && !qs.getFilters().isEmpty()
                && "username".equals(qs.getFilters().get(0).field())
                && dto.username().equals(qs.getFilters().get(0).value()))))
                .thenReturn(true); // Username exists in Registration

        // Mock email does not exist in Person table
        when(dynamicDAO.exists(argThat(qs -> qs != null && Person.class.equals(qs.getRootEntity())
                && qs.getFilters() != null && !qs.getFilters().isEmpty()
                && "email".equals(qs.getFilters().get(0).field())
                && lowerCaseEmail.equals(qs.getFilters().get(0).value()))))
                .thenReturn(false);

        // Mock email does not exist in Registration table
        when(dynamicDAO.exists(argThat(qs -> qs != null && Registration.class.equals(qs.getRootEntity())
                && qs.getFilters() != null && !qs.getFilters().isEmpty()
                && "email".equals(qs.getFilters().get(0).field())
                && lowerCaseEmail.equals(qs.getFilters().get(0).value()))))
                .thenReturn(false);

        // Act
        ServiceResponse<Registration> response = registrationService.newRegistration(dto);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains("Error: Username is already taken!"));
        verify(genericDAO, never()).persist(any());

        // Verify the sequence of calls that lead to failure
        // Username check against User
        verify(dynamicDAO).exists(argThat(qs -> User.class.equals(qs.getRootEntity()) && "username".equals(qs.getFilters().get(0).field())));
        // Username check against Registration (this one returned true)
        verify(dynamicDAO).exists(argThat(qs -> Registration.class.equals(qs.getRootEntity()) && "username".equals(qs.getFilters().get(0).field())));

        // Email checks should still have happened because the username failure doesn't prevent the email block from running
        verify(dynamicDAO).exists(argThat(qs -> Person.class.equals(qs.getRootEntity()) && "email".equals(qs.getFilters().get(0).field())));
        verify(dynamicDAO).exists(argThat(qs -> Registration.class.equals(qs.getRootEntity()) && "email".equals(qs.getFilters().get(0).field())));
    }

    @Test
    void newRegistration_whenEmailExistsInPersonTable_shouldFail() {
        // Arrange
        RegistrationDTO dto = new RegistrationDTO("newUser", "password123", "Test", "User", "existing@example.com");
        String lowerCaseEmail = dto.email().toLowerCase();

        // Mock username is new
        // Check 1: Username in User table
        when(dynamicDAO.exists(argThat(qs -> qs != null && User.class.equals(qs.getRootEntity())
                && qs.getFilters() != null && !qs.getFilters().isEmpty()
                && "username".equals(qs.getFilters().get(0).field())
                && dto.username().equals(qs.getFilters().get(0).value()))))
                .thenReturn(false);
        // Check 2: Username in Registration table
        when(dynamicDAO.exists(argThat(qs -> qs != null && Registration.class.equals(qs.getRootEntity())
                && qs.getFilters() != null && !qs.getFilters().isEmpty()
                && "username".equals(qs.getFilters().get(0).field())
                && dto.username().equals(qs.getFilters().get(0).value()))))
                .thenReturn(false);

        // Mock email exists in Person table
        // Check 3: Email in Person table
        when(dynamicDAO.exists(argThat(qs -> qs != null && Person.class.equals(qs.getRootEntity())
                && qs.getFilters() != null && !qs.getFilters().isEmpty()
                && "email".equals(qs.getFilters().get(0).field())
                && lowerCaseEmail.equals(qs.getFilters().get(0).value()))))
                .thenReturn(true); // Email exists in Person

        // Check 4 (Email in Registration table) will be short-circuited by the OR (||) operator
        // in the service if the previous email check is true. So, no stub needed for it here.

        // Act
        ServiceResponse<Registration> response = registrationService.newRegistration(dto);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains("Error: Email is already in use!"));
        verify(genericDAO, never()).persist(any());

        // Verify calls
        verify(dynamicDAO, times(1)).exists(argThat(qs -> User.class.equals(qs.getRootEntity()) && "username".equals(qs.getFilters().get(0).field())));
        verify(dynamicDAO, times(1)).exists(argThat(qs -> Registration.class.equals(qs.getRootEntity()) && "username".equals(qs.getFilters().get(0).field())));
        verify(dynamicDAO, times(1)).exists(argThat(qs -> Person.class.equals(qs.getRootEntity()) && "email".equals(qs.getFilters().get(0).field())));
        // Verify that the second email check (against Registration) was NOT made due to short-circuiting
        verify(dynamicDAO, never()).exists(argThat(qs -> Registration.class.equals(qs.getRootEntity()) && "email".equals(qs.getFilters().get(0).field()) && !Person.class.equals(qs.getRootEntity()) ));
    }

    @Test
    void newRegistration_whenEmailExistsInRegistrationTable_shouldFail() {
        // Arrange
        RegistrationDTO dto = new RegistrationDTO("newUser", "password123", "Test", "User", "pending@example.com");
        String lowerCaseEmail = dto.email().toLowerCase();

        // Mock username is new
        // Check 1: Username in User table
        when(dynamicDAO.exists(argThat(qs -> qs != null && User.class.equals(qs.getRootEntity())
                && qs.getFilters() != null && !qs.getFilters().isEmpty()
                && "username".equals(qs.getFilters().get(0).field())
                && dto.username().equals(qs.getFilters().get(0).value()))))
                .thenReturn(false);
        // Check 2: Username in Registration table
        when(dynamicDAO.exists(argThat(qs -> qs != null && Registration.class.equals(qs.getRootEntity())
                && qs.getFilters() != null && !qs.getFilters().isEmpty()
                && "username".equals(qs.getFilters().get(0).field())
                && dto.username().equals(qs.getFilters().get(0).value()))))
                .thenReturn(false);

        // Mock email does not exist in Person table
        // Check 3: Email in Person table
        when(dynamicDAO.exists(argThat(qs -> qs != null && Person.class.equals(qs.getRootEntity())
                && qs.getFilters() != null && !qs.getFilters().isEmpty()
                && "email".equals(qs.getFilters().get(0).field())
                && lowerCaseEmail.equals(qs.getFilters().get(0).value()))))
                .thenReturn(false);

        // Mock email exists in Registration table
        // Check 4: Email in Registration table
        when(dynamicDAO.exists(argThat(qs -> qs != null && Registration.class.equals(qs.getRootEntity())
                && qs.getFilters() != null && !qs.getFilters().isEmpty()
                && "email".equals(qs.getFilters().get(0).field())
                && lowerCaseEmail.equals(qs.getFilters().get(0).value()))))
                .thenReturn(true); // Email exists in Registration

        // Act
        ServiceResponse<Registration> response = registrationService.newRegistration(dto);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains("Error: Email is already in use!"));
        verify(genericDAO, never()).persist(any());

        // Verify all four checks were made
        verify(dynamicDAO, times(1)).exists(argThat(qs -> User.class.equals(qs.getRootEntity()) && "username".equals(qs.getFilters().get(0).field())));
        verify(dynamicDAO, times(1)).exists(argThat(qs -> Registration.class.equals(qs.getRootEntity()) && "username".equals(qs.getFilters().get(0).field())));
        verify(dynamicDAO, times(1)).exists(argThat(qs -> Person.class.equals(qs.getRootEntity()) && "email".equals(qs.getFilters().get(0).field())));
        verify(dynamicDAO, times(1)).exists(argThat(qs -> Registration.class.equals(qs.getRootEntity()) && "email".equals(qs.getFilters().get(0).field())));
    }
    // endregion

    // region confirmRegistrationEmail Tests
    @Test
    void confirmRegistrationEmail_whenKeyIsValid_shouldSucceedAndCreateUserAndPublishEvent() {
        // Arrange
        String validKey = UUID.randomUUID().toString();
        Registration foundRegistration = new Registration();
        foundRegistration.setUsername("confirmNewUser");
        foundRegistration.setPassword("encodedPasswordConfirm");
        foundRegistration.setEmail("confirmnew@example.com");
        foundRegistration.setFirstName("ConfirmNew");
        foundRegistration.setLastName("UserNew");
        foundRegistration.setRegistrationKey(validKey); // Ensure the key matches

        when(dynamicDAO.<Registration>findOptional(argThat(qs -> qs != null
                && Registration.class.equals(qs.getRootEntity())
                && qs.getFilters().get(0).field().equals("registrationKey")
                && qs.getFilters().get(0).value().equals(validKey))))
                .thenReturn(Optional.of(foundRegistration));

        // Act
        ServiceResponse<User> response = registrationService.confirmRegistrationEmail(validKey);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.SUCCESS, response.getAckCode());
        assertNotNull(response.getDataObject());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(genericDAO).persist(userCaptor.capture());
        User persistedUser = userCaptor.getValue();

        assertEquals(foundRegistration.getUsername(), persistedUser.getUsername());
        assertEquals(foundRegistration.getPassword(), persistedUser.getPassword());
        assertNotNull(persistedUser.getPerson());
        assertEquals(foundRegistration.getEmail(), persistedUser.getPerson().getEmail());
        assertEquals(foundRegistration.getFirstName(), persistedUser.getPerson().getFirstName());
        assertEquals(foundRegistration.getLastName(), persistedUser.getPerson().getLastName());

        ArgumentCaptor<Registration> registrationCaptor = ArgumentCaptor.forClass(Registration.class);
        verify(genericDAO).remove(registrationCaptor.capture());
        assertEquals(foundRegistration, registrationCaptor.getValue());

        // Verify dynamicDAO.findOptional was called once
        verify(dynamicDAO, times(1)).findOptional(any(QuerySpec.class));

        verify(genericDAO).remove(eq(foundRegistration));
        verify(eventPublisher).publishEvent(any(UserCreatedEvent.class)); // Verify event publication
    }

    @Test
    void confirmRegistrationEmail_whenKeyIsInvalid_shouldFail() {
        // Arrange
        String invalidKey = "invalid-key-new";
        when(dynamicDAO.<Registration>findOptional(any(QuerySpec.class))).thenReturn(Optional.empty());

        // Act
        ServiceResponse<User> response = registrationService.confirmRegistrationEmail(invalidKey);

        // Assert
        assertEquals(ServiceResponse.AckCodeType.FAILURE, response.getAckCode());
        assertTrue(response.getMessages().contains("Error: Invalid registration key"));
        verify(genericDAO, never()).persist(any(User.class));
        verify(genericDAO, never()).remove(any(Registration.class));
        verify(eventPublisher, never()).publishEvent(any());
    }
    // endregion
}