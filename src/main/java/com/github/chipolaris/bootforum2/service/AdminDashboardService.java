package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dto.AdminDashboardDTO;
import com.github.chipolaris.bootforum2.dto.SnapshotStatsDTO;
import com.github.chipolaris.bootforum2.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardService.class);

    private final UserRepository userRepository;
    private final ForumRepository forumRepository;
    private final DiscussionRepository discussionRepository;
    private final TagRepository tagRepository;
    private final CommentRepository commentRepository;

    public AdminDashboardService(UserRepository userRepository, ForumRepository forumRepository,
                                 DiscussionRepository discussionRepository, TagRepository tagRepository,
                                 CommentRepository commentRepository) {
        this.userRepository = userRepository;
        this.forumRepository = forumRepository;
        this.discussionRepository = discussionRepository;
        this.tagRepository = tagRepository;
        this.commentRepository = commentRepository;
    }

    public ServiceResponse<AdminDashboardDTO> getDashboardData(String timeWindow) {
        logger.info("Fetching admin dashboard data for time window: {}", timeWindow);
        try {
            AdminDashboardDTO dashboardDTO = new AdminDashboardDTO();
            dashboardDTO.setSnapshotStats(getSnapshotStats());

            LocalDateTime since = calculateSince(timeWindow);
            Pageable top5 = PageRequest.of(0, 5);

            // User rankings
            dashboardDTO.setUsersByDiscussions(userRepository.findTopUsersByDiscussionCount(since, top5));
            dashboardDTO.setUsersByComments(userRepository.findTopUsersByCommentCount(since, top5));
            dashboardDTO.setUsersByReputation(userRepository.findTopUsersByReputation(top5)); // Reputation is all-time

            // Discussion rankings
            dashboardDTO.setDiscussionsByViews(discussionRepository.findTopDiscussionsByViews(since, top5));
            dashboardDTO.setDiscussionsByComments(discussionRepository.findTopDiscussionsByComments(since, top5));

            // Tag rankings
            dashboardDTO.setTagsByViews(tagRepository.findTopTagsByViews(since, top5));
            dashboardDTO.setTagsByComments(tagRepository.findTopTagsByComments(since, top5));

            // Forum rankings
            dashboardDTO.setForumsByViews(forumRepository.findTopForumsByViews(since, top5));
            dashboardDTO.setForumsByComments(forumRepository.findTopForumsByComments(since, top5));

            return ServiceResponse.success("Dashboard data retrieved successfully", dashboardDTO);
        } catch (Exception e) {
            logger.error("Error fetching admin dashboard data", e);
            return ServiceResponse.failure("An unexpected error occurred while fetching dashboard data.");
        }
    }

    private SnapshotStatsDTO getSnapshotStats() {
        long memberCount = userRepository.count();
        long forumCount = forumRepository.count();
        long discussionCount = discussionRepository.count();
        long tagCount = tagRepository.count();
        long commentCount = commentRepository.count();

        // Calculate total image count from discussions and comments
        Long discussionImageCount = discussionRepository.sumImageCount();
        long commentImageCount = commentRepository.countAllImages();
        long totalImageCount = (discussionImageCount != null ? discussionImageCount : 0L) + commentImageCount;

        // Calculate total attachment count from discussions and comments
        Long discussionAttachmentCount = discussionRepository.sumAttachmentCount();
        long commentAttachmentCount = commentRepository.countAllAttachments();
        long totalAttachmentCount = (discussionAttachmentCount != null ? discussionAttachmentCount : 0L) + commentAttachmentCount;

        return new SnapshotStatsDTO(memberCount, forumCount, discussionCount, tagCount, commentCount,
                totalAttachmentCount, totalImageCount);
    }

    private LocalDateTime calculateSince(String timeWindow) {
        return switch (timeWindow) {
            case "7d" -> LocalDateTime.now().minusDays(7);
            case "30d" -> LocalDateTime.now().minusMonths(1);
            case "1y" -> LocalDateTime.now().minusYears(1);
            default -> LocalDateTime.of(1970, 1, 1, 0, 0); // "all"
        };
    }
}
