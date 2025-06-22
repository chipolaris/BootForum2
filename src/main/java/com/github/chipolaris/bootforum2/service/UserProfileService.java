package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.domain.Comment;
import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.dto.UserProfileCommentDTO;
import com.github.chipolaris.bootforum2.dto.UserProfileDTO;
import com.github.chipolaris.bootforum2.dto.UserProfileDiscussionDTO;
import com.github.chipolaris.bootforum2.event.UserProfileViewedEvent;
import com.github.chipolaris.bootforum2.mapper.UserProfileMapper;
import com.github.chipolaris.bootforum2.repository.CommentRepository;
import com.github.chipolaris.bootforum2.repository.DiscussionRepository;
import com.github.chipolaris.bootforum2.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserRepository userRepository;
    private final DiscussionRepository discussionRepository;
    private final CommentRepository commentRepository;
    private final UserProfileMapper userProfileMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public UserProfileService(UserRepository userRepository, DiscussionRepository discussionRepository,
                              CommentRepository commentRepository, UserProfileMapper userProfileMapper,
                              ApplicationEventPublisher applicationEventPublisher) {
        this.userRepository = userRepository;
        this.discussionRepository = discussionRepository;
        this.commentRepository = commentRepository;
        this.userProfileMapper = userProfileMapper;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public ServiceResponse<UserProfileDTO> getUserProfile(String username) {

        User user = userRepository.findByUsername(username);

        if (user == null) {
            return ServiceResponse.failure("User not found: '%s'".formatted(username));
        }

        // Fetch latest discussions
        List<Discussion> latestDiscussions = discussionRepository.findTop5ByCreateByOrderByCreateDateDesc(username);
        List<UserProfileDiscussionDTO> discussionDTOs = latestDiscussions.stream()
                .map(userProfileMapper::discussionToUserProfileDiscussionDTO)
                .collect(Collectors.toList());

        // Fetch latest comments
        List<Comment> latestComments = commentRepository.findTop10ByCreateByOrderByCreateDateDesc(username);
        List<UserProfileCommentDTO> commentDTOs = latestComments.stream()
                .map(userProfileMapper::commentToUserProfileCommentDTO)
                .collect(Collectors.toList());

        // Map to final DTO
        UserProfileDTO userProfileDTO = userProfileMapper.toUserProfileDTO(user, discussionDTOs, commentDTOs);

        applicationEventPublisher.publishEvent(new UserProfileViewedEvent(this, username));

        return ServiceResponse.success("User profile retrieved successfully", userProfileDTO);
    }
}