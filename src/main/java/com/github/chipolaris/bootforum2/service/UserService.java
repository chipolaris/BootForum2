package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.dto.*;
import com.github.chipolaris.bootforum2.dto.AdminPasswordChangeDTO;
import com.github.chipolaris.bootforum2.dto.AdminUserUpdateDTO;
import com.github.chipolaris.bootforum2.dto.UserSummaryDTO;
import com.github.chipolaris.bootforum2.enumeration.AccountStatus;
import com.github.chipolaris.bootforum2.enumeration.UserRole;
import com.github.chipolaris.bootforum2.mapper.PersonMapper;
import com.github.chipolaris.bootforum2.mapper.UserMapper;
import com.github.chipolaris.bootforum2.repository.CommentRepository;
import com.github.chipolaris.bootforum2.repository.DiscussionRepository;
import com.github.chipolaris.bootforum2.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

	private static final Logger logger = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;
	private final UserMapper userMapper;
	private final DiscussionRepository discussionRepository;
	private final CommentRepository commentRepository;
	private final PersonMapper personMapper;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationFacade authenticationFacade;

	public UserService(UserRepository userRepository, DiscussionRepository discussionRepository,
					   CommentRepository commentRepository, UserMapper userMapper,
					   PersonMapper personMapper, PasswordEncoder passwordEncoder,
					   AuthenticationFacade authenticationFacade) {
		this.userRepository = userRepository;
		this.discussionRepository = discussionRepository;
		this.commentRepository = commentRepository;
		this.userMapper = userMapper;
		this.personMapper = personMapper;
		this.passwordEncoder = passwordEncoder;
		this.authenticationFacade = authenticationFacade;
	}

	@Transactional(readOnly = true)
	public ServiceResponse<Page<UserSummaryDTO>> getUsers(Pageable pageable) {
		try {
			Page<User> userPage = userRepository.findAll(pageable);
			Page<UserSummaryDTO> dtoPage = userPage.map(userMapper::toUserSummaryDTO);
			return ServiceResponse.success("Users retrieved successfully", dtoPage);
		} catch (Exception e) {
			logger.error("Error retrieving users", e);
			return ServiceResponse.failure("An unexpected error occurred while retrieving users.");
		}
	}

	@Transactional(readOnly = true)
	public ServiceResponse<UserDTO> getUser(String username) {
		return userRepository.findByUsername(username)
				.map(user -> ServiceResponse.success("User found", userMapper.toDTO(user)))
				.orElse(ServiceResponse.failure("User not found: " + username));
	}

	public ServiceResponse<Void> updateUserByAdmin(Long userId, AdminUserUpdateDTO updateDTO) {
		Optional<User> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			return ServiceResponse.failure("User not found with ID: " + userId);
		}

		User user = userOpt.get();
		String adminUsername = authenticationFacade.getCurrentUsername().orElse("system");

		try {
			// Update roles
			Set<UserRole> newRoles = updateDTO.roles().stream()
					.map(UserRole::valueOf)
					.collect(Collectors.toSet());
			user.setUserRoles(newRoles);

			// Update status
			user.setAccountStatus(AccountStatus.valueOf(updateDTO.accountStatus()));
			user.setUpdateBy(adminUsername);
			userRepository.save(user);
			logger.info("Admin '{}' updated user '{}'", adminUsername, user.getUsername());
			return ServiceResponse.success("User updated successfully.");

		} catch (IllegalArgumentException e) {
			logger.warn("Invalid role or status value provided by admin '{}' for user '{}'", adminUsername, user.getUsername());
			return ServiceResponse.failure("Invalid role or status value provided.");
		}
	}

	public ServiceResponse<Void> changePasswordByAdmin(Long userId, AdminPasswordChangeDTO passwordDTO) {
		Optional<User> userOpt = userRepository.findById(userId);
		if (userOpt.isEmpty()) {
			return ServiceResponse.failure("User not found with ID: " + userId);
		}

		User user = userOpt.get();
		String adminUsername = authenticationFacade.getCurrentUsername().orElse("system");

		user.setPassword(passwordEncoder.encode(passwordDTO.newPassword()));
		user.setUpdateBy(adminUsername);
		userRepository.save(user);
		logger.info("Admin '{}' changed password for user '{}'", adminUsername, user.getUsername());

		return ServiceResponse.success("Password changed successfully.");
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
	 * @param passwordChangeDTO The DTO containing old, new, and confirmation passwords.
	 * @return A ServiceResponse indicating success or failure.
	 */
	public ServiceResponse<Void> updatePassword(PasswordChangeDTO passwordChangeDTO) {

		Optional<String> currentUsernameOpt = authenticationFacade.getCurrentUsername();
		if (currentUsernameOpt.isEmpty()) {
			return ServiceResponse.failure("User not authenticated.");
		}
		String username = currentUsernameOpt.get();

		// Add validation for password match
		if (!passwordChangeDTO.newPassword().equals(passwordChangeDTO.confirmNewPassword())) {
			return ServiceResponse.failure("New password and confirmation do not match.");
		}

		Optional<User> userOpt = userRepository.findByUsername(username);
		if (userOpt.isEmpty()) {
			return ServiceResponse.failure("Authenticated user could not be found in the database.");
		}

		User user = userOpt.get();

		// Verify the old password
		if (!passwordEncoder.matches(passwordChangeDTO.oldPassword(), user.getPassword())) {
			return ServiceResponse.failure("Incorrect old password.");
		}

		// Encode and set the new password
		user.setPassword(passwordEncoder.encode(passwordChangeDTO.newPassword()));
		user.setUpdateBy(username); // Set the updater

		userRepository.save(user);
		logger.info("Successfully updated password for user '{}'", username);

		return ServiceResponse.success("Password updated successfully.");
	}

	@Transactional(readOnly = true)
	public ServiceResponse<MyActivitiesDTO> getMyActivities(String username) {

		try {
			// Define a page request to limit results, e.g., top 10 recent items
			Pageable pageable = PageRequest.of(0, 10, Sort.by("createDate").descending());

			List<MyRecentDiscussionDTO> recentDiscussions = discussionRepository.findRecentDiscussionsForUser(username, pageable);
			List<MyRecentCommentDTO> recentComments = commentRepository.findRecentCommentsForUser(username, pageable);
			List<ReplyToMyCommentDTO> repliesToMyComments = commentRepository.findRepliesToUserComments(username, pageable);
			List<MyLikedDiscussionDTO> likedDiscussions = discussionRepository.findLikedDiscussionsByUser(username, pageable);
			List<MyLikedCommentDTO> likedComments = commentRepository.findLikedCommentsByUser(username, pageable);

			MyActivitiesDTO myActivitiesDTO = new MyActivitiesDTO();
			myActivitiesDTO.setRecentDiscussions(recentDiscussions);
			myActivitiesDTO.setRecentComments(recentComments);
			myActivitiesDTO.setRepliesToMyComments(repliesToMyComments);
			myActivitiesDTO.setLikedDiscussions(likedDiscussions);
			myActivitiesDTO.setLikedComments(likedComments);

			return ServiceResponse.success("Successfully retrieved user activities.", myActivitiesDTO);

		} catch (Exception e) {
			logger.error("Error retrieving activities for user " + username, e);
			return ServiceResponse.failure("An unexpected error occurred while retrieving your activities.");
		}
	}
	@Transactional(readOnly = true)
	public ServiceResponse<UserReputationDTO> getUserReputation(String username) {

		try {
			Optional<User> userOpt = userRepository.findByUsername(username);
			if (userOpt.isEmpty()) {
				return ServiceResponse.failure("User not found: " + username);
			}
			User user = userOpt.get();

			UserReputationDTO reputationDTO = new UserReputationDTO();

			// Basic Stats from UserStat
			reputationDTO.setProfileViewCount(user.getStat().getProfileViewed());
			reputationDTO.setTotalDiscussions(user.getStat().getDiscussionCount());
			reputationDTO.setTotalComments(user.getStat().getCommentCount());

			// Aggregate Vote Counts
			long discussionUpVotes = Optional.ofNullable(discussionRepository.sumVoteUpCountByCreateBy(username)).orElse(0L);
			long discussionDownVotes = Optional.ofNullable(discussionRepository.sumVoteDownCountByCreateBy(username)).orElse(0L);
			long commentUpVotes = Optional.ofNullable(commentRepository.sumVoteUpCountByCreateBy(username)).orElse(0L);
			long commentDownVotes = Optional.ofNullable(commentRepository.sumVoteDownCountByCreateBy(username)).orElse(0L);

			reputationDTO.setTotalUpVotes(discussionUpVotes + commentUpVotes);
			reputationDTO.setTotalDownVotes(discussionDownVotes + commentDownVotes);

			Pageable topTen = PageRequest.of(0, 10);

			// Ranked Lists
			reputationDTO.setMostViewedDiscussions(discussionRepository.findMostViewedDiscussionsForUser(username, topTen));
			reputationDTO.setMostLikedDiscussions(discussionRepository.findMostLikedDiscussionsForUser(username, topTen));
			reputationDTO.setMostDislikedDiscussions(discussionRepository.findMostDislikedDiscussionsForUser(username, topTen));
			reputationDTO.setMostNetLikedDiscussions(discussionRepository.findMostNetLikedDiscussionsForUser(username, topTen));

			reputationDTO.setMostLikedComments(commentRepository.findMostLikedCommentsForUser(username,
					PageRequest.of(0, 10, Sort.by("commentVote.voteUpCount").descending())));
			reputationDTO.setMostDislikedComments(commentRepository.findMostDislikedCommentsForUser(username,
					PageRequest.of(0, 10, Sort.by("commentVote.voteDownCount").descending())));

			return ServiceResponse.success("Successfully retrieved user reputation.", reputationDTO);

		} catch (Exception e) {
			logger.error("Error retrieving reputation for user " + username, e);
			return ServiceResponse.failure("An unexpected error occurred while retrieving your reputation data.");
		}
	}

}