package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dto.admin.*;
import com.github.chipolaris.bootforum2.repository.CommentRepository;
import com.github.chipolaris.bootforum2.repository.DiscussionRepository;
import com.github.chipolaris.bootforum2.repository.ForumRepository;
import com.github.chipolaris.bootforum2.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminChartService {

    private static final Logger logger = LoggerFactory.getLogger(AdminChartService.class);

    private final DiscussionRepository discussionRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ForumRepository forumRepository;

    public AdminChartService(DiscussionRepository discussionRepository, CommentRepository commentRepository,
                             UserRepository userRepository, ForumRepository forumRepository) {
        this.discussionRepository = discussionRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.forumRepository = forumRepository;
    }

    public ServiceResponse<AdminChartDTO> getChartData() {
        logger.info("Fetching data for admin charts");
        try {
            ChartDataDTO contentActivity = buildContentActivityChart();
            ChartDataDTO newUsers = buildNewUsersChart();
            ChartDataDTO forumActivity = buildForumActivityChart();

            AdminChartDTO adminChartDTO = new AdminChartDTO(contentActivity, newUsers, forumActivity);
            return ServiceResponse.success("Chart data retrieved successfully", adminChartDTO);
        } catch (Exception e) {
            logger.error("Failed to retrieve chart data", e);
            return ServiceResponse.failure("An unexpected error occurred while retrieving chart data.");
        }
    }

    private ChartDataDTO buildContentActivityChart() {
        LocalDateTime twelveMonthsAgo = LocalDate.now().minusMonths(11).withDayOfMonth(1).atStartOfDay();
        List<CountPerMonthDTO> discussionCounts = discussionRepository.countByMonth(twelveMonthsAgo);
        List<CountPerMonthDTO> commentCounts = commentRepository.countByMonth(twelveMonthsAgo);

        Map<YearMonth, Long> discussionMap = discussionCounts.stream()
                .collect(Collectors.toMap(dto -> YearMonth.of(dto.year(), dto.month()), CountPerMonthDTO::count));
        Map<YearMonth, Long> commentMap = commentCounts.stream()
                .collect(Collectors.toMap(dto -> YearMonth.of(dto.year(), dto.month()), CountPerMonthDTO::count));

        List<String> labels = new ArrayList<>();
        List<Number> discussionData = new ArrayList<>();
        List<Number> commentData = new ArrayList<>();

        YearMonth currentMonth = YearMonth.from(LocalDate.now().minusMonths(11));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");

        for (int i = 0; i < 12; i++) {
            labels.add(currentMonth.format(formatter));
            discussionData.add(discussionMap.getOrDefault(currentMonth, 0L));
            commentData.add(commentMap.getOrDefault(currentMonth, 0L));
            currentMonth = currentMonth.plusMonths(1);
        }

        List<ChartDataSetDTO> datasets = List.of(
                new ChartDataSetDTO("Discussions", discussionData),
                new ChartDataSetDTO("Comments", commentData)
        );

        return new ChartDataDTO(labels, datasets);
    }

    private ChartDataDTO buildNewUsersChart() {
        LocalDateTime twelveMonthsAgo = LocalDate.now().minusMonths(11).withDayOfMonth(1).atStartOfDay();
        List<CountPerMonthDTO> userCounts = userRepository.countByMonth(twelveMonthsAgo);
        Map<YearMonth, Long> userMap = userCounts.stream()
                .collect(Collectors.toMap(dto -> YearMonth.of(dto.year(), dto.month()), CountPerMonthDTO::count));

        List<String> labels = new ArrayList<>();
        List<Number> userData = new ArrayList<>();

        YearMonth currentMonth = YearMonth.from(LocalDate.now().minusMonths(11));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");

        for (int i = 0; i < 12; i++) {
            labels.add(currentMonth.format(formatter));
            userData.add(userMap.getOrDefault(currentMonth, 0L));
            currentMonth = currentMonth.plusMonths(1);
        }

        List<ChartDataSetDTO> datasets = List.of(new ChartDataSetDTO("New Users", userData));
        return new ChartDataDTO(labels, datasets);
    }

    private ChartDataDTO buildForumActivityChart() {
        List<ForumActivityDTO> topForums = forumRepository.findTopForumActivity(PageRequest.of(0, 10));

        List<String> labels = topForums.stream().map(ForumActivityDTO::forumName).collect(Collectors.toList());
        List<Number> discussionData = topForums.stream().map(f -> (Number) f.discussionCount()).collect(Collectors.toList());
        List<Number> commentData = topForums.stream().map(f -> (Number) f.commentCount()).collect(Collectors.toList());

        List<ChartDataSetDTO> datasets = List.of(
                new ChartDataSetDTO("Discussions", discussionData),
                new ChartDataSetDTO("Comments", commentData)
        );

        return new ChartDataDTO(labels, datasets);
    }
}