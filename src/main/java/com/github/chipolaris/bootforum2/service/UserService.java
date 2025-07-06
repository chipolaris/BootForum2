package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.dto.PersonUpdateDTO;
import com.github.chipolaris.bootforum2.dto.UserDTO;
import com.github.chipolaris.bootforum2.mapper.PersonMapper;
import com.github.chipolaris.bootforum2.mapper.UserMapper;
import com.github.chipolaris.bootforum2.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService {

	private static final Logger logger = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final PersonMapper personMapper; // Added
	private final PasswordEncoder passwordEncoder; // Added
	private final AuthenticationFacade authenticationFacade; // Added

	public UserService(UserRepository userRepository, UserMapper userMapper,
					   PersonMapper personMapper, PasswordEncoder passwordEncoder,
					   AuthenticationFacade authenticationFacade) {
		this.userRepository = userRepository;
		this.userMapper = userMapper;
		this.personMapper = personMapper;
		this.passwordEncoder = passwordEncoder;
		this.authenticationFacade = authenticationFacade;
	}

	@Transactional(readOnly = true)
	public ServiceResponse<UserDTO> getUser(String username) {
		return userRepository.findByUsername(username)
				.map(user -> ServiceResponse.success("User found", userMapper.toDTO(user)))
				.orElse(ServiceResponse.failure("User not found: " + username));
	}

	/**
	 * Updates the personal information (first name, last name, email) for the currently authenticated user.
	 *
	 * @param personUpateDTO DTO containing the new personal information.
	 * @return A ServiceResponse containing the updated UserDTO.
	 */
	public ServiceResponse<UserDTO> updatePersonInfo(PersonUpdateDTO personUpateDTO) {

		Optional<String> currentUsernameOpt = authenticationFacade.getCurrentUsername();
		if (currentUsernameOpt.isEmpty()) {
			return ServiceResponse.failure("User not authenticated.");
		}
		String username = currentUsernameOpt.get();

		Optional<User> userOpt = userRepository.findByUsername(username);
		if (userOpt.isEmpty()) {
			return ServiceResponse.failure("Authenticated user could not be found in the database.");
		}

		User user = userOpt.get();
		personMapper.updatePersonFromDTO(personUpateDTO, user.getPerson());
		user.setUpdateBy(username); // Set the updater

		User updatedUser = userRepository.save(user);
		logger.info("Successfully updated person info for user '{}'", username);

		return ServiceResponse.success("Personal information updated successfully.", userMapper.toDTO(updatedUser));
	}

	/**
	 * Updates the password for the currently authenticated user after verifying the old password.
	 *
	 * @param oldPassword The user's current password.
	 * @param newPassword The new password to set.
	 * @return A ServiceResponse indicating success or failure.
	 */
	public ServiceResponse<Void> updatePassword(String oldPassword, String newPassword) {

		Optional<String> currentUsernameOpt = authenticationFacade.getCurrentUsername();
		if (currentUsernameOpt.isEmpty()) {
			return ServiceResponse.failure("User not authenticated.");
		}
		String username = currentUsernameOpt.get();

		Optional<User> userOpt = userRepository.findByUsername(username);
		if (userOpt.isEmpty()) {
			return ServiceResponse.failure("Authenticated user could not be found in the database.");
		}

		User user = userOpt.get();

		// Verify the old password
		if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
			return ServiceResponse.failure("Incorrect old password.");
		}

		// Encode and set the new password
		user.setPassword(passwordEncoder.encode(newPassword));
		user.setUpdateBy(username); // Set the updater

		userRepository.save(user);
		logger.info("Successfully updated password for user '{}'", username);

		return ServiceResponse.success("Password updated successfully.");
	}
}