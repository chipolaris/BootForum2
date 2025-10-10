package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dto.KeywordCountDTO;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminChartService {

    private static final Logger logger = LoggerFactory.getLogger(AdminChartService.class);

    private final DiscussionRepository discussionRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ForumRepository forumRepository;
    private final DiscussionService discussionService;

    public AdminChartService(DiscussionRepository discussionRepository, CommentRepository commentRepository,
                             UserRepository userRepository, ForumRepository forumRepository,
                             DiscussionService discussionService) {
        this.discussionRepository = discussionRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.forumRepository = forumRepository;
        this.discussionService = discussionService;
    }

    public ServiceResponse<AdminChartDTO> getChartData() {
        logger.info("Fetching data for admin charts");
        try {
            ChartDataDTO contentActivity = buildContentActivityChart();
            ChartDataDTO newUsers = buildNewUsersChart();
            ChartDataDTO forumActivity = buildForumActivityChart();
            //ChartDataDTO topTerms = buildDiscussionTopTermsChart();

            AdminChartDTO adminChartDTO = new AdminChartDTO(contentActivity, newUsers, forumActivity, null);
            return ServiceResponse.success("Chart data retrieved successfully", adminChartDTO);
        } catch (Exception e) {
            logger.error("Failed to retrieve chart data", e);
            return ServiceResponse.failure("An unexpected error occurred while retrieving chart data.");
        }
    }
    /**
     * Gets only the data for the top terms chart.
     */
    public ServiceResponse<ChartDataDTO> getTopTermsChartData(int limit, String period) {
        logger.info("Fetching data for top terms chart");
        try {
            ChartDataDTO topTerms = buildDiscussionTopTermsChart(limit, period);
            return ServiceResponse.success("Top terms chart data retrieved successfully", topTerms);
        } catch (Exception e) {
            logger.error("Failed to retrieve top terms chart data", e);
            return ServiceResponse.failure("An unexpected error occurred while retrieving top terms chart data.");
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

    private ChartDataDTO buildDiscussionTopTermsChart(int limit, String period) {

        // 1. Get top terms for Discussion from "title" and "content" fields
        ServiceResponse<List<KeywordCountDTO>> discussionTitleTermsResponse = discussionService.getTopTerms("title_terms", limit, period);
        Map<String, Long> titleTermMap = new HashMap<>();
        if (discussionTitleTermsResponse.isSuccess() && discussionTitleTermsResponse.getDataObject() != null) {
            titleTermMap = discussionTitleTermsResponse.getDataObject().stream()
                    .collect(Collectors.toMap(KeywordCountDTO::keyword, KeywordCountDTO::count));
        }

        ServiceResponse<List<KeywordCountDTO>> discussionContentTermsResponse = discussionService.getTopTerms("content_terms", limit, period);
        Map<String, Long> contentTermMap = new HashMap<>();
        if (discussionContentTermsResponse.isSuccess() && discussionContentTermsResponse.getDataObject() != null) {
            contentTermMap = discussionContentTermsResponse.getDataObject().stream()
                    .collect(Collectors.toMap(KeywordCountDTO::keyword, KeywordCountDTO::count));
        }

        // 2. Combine and find the top N overall terms
        Map<String, Long> combinedTermTotals = new HashMap<>(titleTermMap);
        contentTermMap.forEach((keyword, count) -> combinedTermTotals.merge(keyword, count, Long::sum));

        List<String> topLabels = combinedTermTotals.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 3. Build datasets based on the final sorted labels
        List<Number> titleData = new ArrayList<>();
        List<Number> contentData = new ArrayList<>();

        for (String label : topLabels) {
            titleData.add(titleTermMap.getOrDefault(label, 0L));
            contentData.add(contentTermMap.getOrDefault(label, 0L));
        }

        List<ChartDataSetDTO> datasets = List.of(
                new ChartDataSetDTO("DiscussionTitle", titleData),
                new ChartDataSetDTO("DiscussionContent", contentData)
        );

        return new ChartDataDTO(topLabels, datasets);
    }
}