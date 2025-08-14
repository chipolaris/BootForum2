package com.github.chipolaris.bootforum2.rest;

import com.github.chipolaris.bootforum2.dto.*;
import com.github.chipolaris.bootforum2.service.ServiceResponse;
import com.github.chipolaris.bootforum2.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

	private static final Logger logger = LoggerFactory.getLogger(UserController.class);
	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	/**
	 * Get current user's profile
	 * @param userDetails
	 * @return
	 */
	@GetMapping("/my-profile")
	public ApiResponse<?> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
		// @AuthenticationPrincipal injects the principal object (UserDetails by default)
		if (userDetails == null) {
			// Should not happen if endpoint is secured correctly, but good practice
			return ApiResponse.error("User not authenticated");
		}
		// Return the UserDetails object or map it to a DTO
		// Avoid sending sensitive info like passwords!
		UserDTO user = userService.getUser(userDetails.getUsername()).getDataObject();
		return ApiResponse.success(user);
	}

	/**
	 * Updates the personal information for the authenticated user.
	 *
	 * @param personUpdateDTO The DTO with the updated first name, last name, and email.
	 * @return A ResponseEntity containing the updated UserDTO or an error.
	 */
	@PutMapping("/my-profile/update-person")
	public ApiResponse<?> updatePerson(@Valid @RequestBody PersonUpdateDTO personUpdateDTO) {

		ServiceResponse<UserDTO> serviceResponse = userService.updatePersonInfo(personUpdateDTO);

		if (serviceResponse.isSuccess()) {
			return ApiResponse.success(serviceResponse.getDataObject(), "Personal information updated successfully.");
		}

		return ApiResponse.error(serviceResponse.getMessages(), "Failed to update personal information.");
	}

	/**
	 * Changes the password for the authenticated user.
	 *
	 * @param passwordChangeDTO The DTO containing the old and new passwords.
	 * @return A ResponseEntity indicating success or failure.
	 */
	@PostMapping("/my-profile/change-password")
	public ApiResponse<?> changePassword(@Valid @RequestBody PasswordChangeDTO passwordChangeDTO) {

		ServiceResponse<Void> serviceResponse = userService.updatePassword(passwordChangeDTO);

		if (serviceResponse.isSuccess()) {
			return ApiResponse.success("Password changed successfully");
		}

		return ApiResponse.error(serviceResponse.getMessages(), "Failed to change password.");
	}

	@GetMapping("/my-activities")
	public ApiResponse<?> getMyActivities(@AuthenticationPrincipal UserDetails userDetails) {
		if (userDetails == null) {
			return ApiResponse.error("User not authenticated");
		}
		String username = userDetails.getUsername();
		logger.info("Fetching activities for user '{}'", username);

		ServiceResponse<MyActivitiesDTO> serviceResponse = userService.getMyActivities(username);

		if (serviceResponse.isSuccess()) {
			return ApiResponse.success(serviceResponse.getDataObject(), "Activities retrieved successfully.");
		}

		return ApiResponse.error(serviceResponse.getMessages(), "Failed to retrieve activities.");
	}
}