package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.*;
import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.domain.FileInfo;
import com.github.chipolaris.bootforum2.domain.Forum;
import com.github.chipolaris.bootforum2.domain.Tag;
import com.github.chipolaris.bootforum2.dto.*;
import com.github.chipolaris.bootforum2.event.DiscussionCreatedEvent;
import com.github.chipolaris.bootforum2.event.DiscussionViewedEvent;
import com.github.chipolaris.bootforum2.mapper.DiscussionMapper;
import com.github.chipolaris.bootforum2.mapper.FileInfoMapper;
import com.github.chipolaris.bootforum2.repository.DiscussionRepository;
import com.github.chipolaris.bootforum2.repository.TagRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiscussionService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussionService.class);

    private final EntityManager entityManager;
    private final GenericDAO genericDAO;
    private final DynamicDAO dynamicDAO;
    private final DiscussionRepository discussionRepository;
    private final TagRepository tagRepository;
    private final DiscussionMapper discussionMapper;
    private final FileService fileService;
    private final FileInfoMapper fileInfoMapper; // To map FileInfoDTO from FileStorageService to FileInfo entity
    private final AuthenticationFacade authenticationFacade;
    private final ApplicationEventPublisher eventPublisher;
    private final ForumSettingService forumSettingService;

    // Note: in Spring version >= 4.3, @AutoWired is implied for beans with single constructor
    public DiscussionService(EntityManager entityManager, GenericDAO genericDAO,
                             DynamicDAO dynamicDAO, DiscussionRepository discussionRepository,
                             TagRepository tagRepository, DiscussionMapper discussionMapper,
                             FileService fileService, FileInfoMapper fileInfoMapper,
                             AuthenticationFacade authenticationFacade, ApplicationEventPublisher eventPublisher,
                             ForumSettingService forumSettingService) {
        this.entityManager = entityManager;
        this.genericDAO = genericDAO;
        this.dynamicDAO = dynamicDAO;
        this.discussionRepository = discussionRepository;
        this.tagRepository = tagRepository;
        this.discussionMapper = discussionMapper;
        this.fileService = fileService;
        this.fileInfoMapper = fileInfoMapper;
        this.authenticationFacade = authenticationFacade;
        this.eventPublisher = eventPublisher;
        this.forumSettingService = forumSettingService;
    }

    @Transactional(rollbackFor = Exception.class, readOnly = false)
    public ServiceResponse<DiscussionDTO> createDiscussion(
            DiscussionCreateDTO discussionCreateDTO,
            MultipartFile[] images,
            MultipartFile[] attachments) {

        logger.info("Creating discussion: {}", discussionCreateDTO.title());

        List<String> validationErrors = new ArrayList<>();
        validateContent(discussionCreateDTO.content(), validationErrors);
        validateTags(discussionCreateDTO.tagIds(), validationErrors);
        validateFiles(images, "images", validationErrors);
        validateFiles(attachments, "attachments", validationErrors);

        if (!validationErrors.isEmpty()) {
            logger.warn("Discussion creation failed due to validation errors: {}", validationErrors);
            return ServiceResponse.failure(String.join(", ", validationErrors));
        }

        String username = authenticationFacade.getCurrentUsername().orElse("system");

        try {
            // 1. Fetch Forum
            Forum forum = genericDAO.find(Forum.class, discussionCreateDTO.forumId());
            if (forum == null) {
                // ... error handling ...
                return ServiceResponse.failure("Forum not found. Cannot create discussion.");
            }

            // 2. create Discussion entity
            Discussion discussion = Discussion.newDiscussion();
            discussion.setTitle(discussionCreateDTO.title());
            discussion.setContent(discussionCreateDTO.content());
            discussion.setForum(forum);
            discussion.setCreateBy(username); // Set creator here

            // 3. Handle Tags
            if (discussionCreateDTO.tagIds() != null && !discussionCreateDTO.tagIds().isEmpty()) {
                List<Tag> tags = tagRepository.findAllById(discussionCreateDTO.tagIds());
                discussion.setTags(new HashSet<>(tags));
                logger.info("Associated {} tags with new discussion", tags.size());
            }

            // 4. Process Files
            List<FileInfo> imageInfos = processFiles(images, "image");
            discussion.setImages(imageInfos);

            List<FileInfo> attachmentInfos = processFiles(attachments, "attachment");
            discussion.setAttachments(attachmentInfos);

            // 5. Persist Discussion
            genericDAO.persist(discussion);

            // 6. Update Forum Statistics (Candidate for Spring Event)
            logger.info("Successfully created discussion '{}' with ID {}", discussion.getTitle(), discussion.getId());
            eventPublisher.publishEvent(new DiscussionCreatedEvent(this, discussion));

            // 7. Map persisted Discussion to DTO for response
            DiscussionDTO discussionDTO = discussionMapper.toDiscussionDTO(discussion);
            return ServiceResponse.success("Discussion created successfully.", discussionDTO);

        } catch (Exception e) {
            logger.error("Error creating discussion: " + discussionCreateDTO.title(), e);
            return ServiceResponse.failure("An unexpected error occurred while creating the discussion: %s".formatted(e.getMessage()));
        }
    }

    private void validateContent(String content, List<String> errors) {
        if (content == null || content.isBlank()) {
            errors.add("Content cannot be empty.");
            return;
        }

        // Using getBytes() to be consistent with frontend's TextEncoder().encode().length
        int contentLength = content.getBytes().length;

        ServiceResponse<Object> minLengthResponse = forumSettingService.getSettingValue("content", "posts.minLength");
        if (minLengthResponse.isSuccess() && minLengthResponse.getDataObject() instanceof Number) {
            int minLength = ((Number) minLengthResponse.getDataObject()).intValue();
            if (contentLength < minLength) {
                errors.add(String.format("Content is too short. Minimum length is %d characters (bytes).", minLength));
            }
        }

        ServiceResponse<Object> maxLengthResponse = forumSettingService.getSettingValue("content", "posts.maxLength");
        if (maxLengthResponse.isSuccess() && maxLengthResponse.getDataObject() instanceof Number) {
            int maxLength = ((Number) maxLengthResponse.getDataObject()).intValue();
            if (contentLength > maxLength) {
                errors.add(String.format("Content is too long. Maximum length is %d characters (bytes).", maxLength));
            }
        }
    }

    private void validateTags(List<Long> tagIds, List<String> errors) {
        if (tagIds == null || tagIds.isEmpty()) {
            return; // No tags to validate
        }

        ServiceResponse<Object> response = forumSettingService.getSettingValue("content", "tags.maxTagsPerPost");
        if (response.isSuccess() && response.getDataObject() instanceof Number) {
            int maxTags = ((Number) response.getDataObject()).intValue();
            if (tagIds.size() > maxTags) {
                errors.add(String.format("You can select a maximum of %d tags.", maxTags));
            }
        }
    }

    private void validateFiles(MultipartFile[] files, String category, List<String> errors) {
        if (files == null || files.length == 0) {
            return; // No files to validate
        }

        // Check if the file category (e.g., images, attachments) is enabled
        ServiceResponse<Object> enabledResponse = forumSettingService.getSettingValue(category, "enabled");
        if (enabledResponse.isSuccess() && enabledResponse.getDataObject() instanceof Boolean && !((Boolean) enabledResponse.getDataObject())) {
            errors.add(String.format("%s uploads are disabled.", capitalize(category)));
            return; // No need to check further if the category is disabled
        }

        // Get file size and type validation rules from settings
        long maxSizeBytes = -1;
        ServiceResponse<Object> sizeResponse = forumSettingService.getSettingValue(category, "maxFileSizeMB");
        if (sizeResponse.isSuccess() && sizeResponse.getDataObject() instanceof Number) {
            maxSizeBytes = ((Number) sizeResponse.getDataObject()).longValue() * 1024 * 1024;
        }

        List<String> allowedTypes = Collections.emptyList();
        ServiceResponse<Object> typesResponse = forumSettingService.getSettingValue(category, "allowedTypes");
        if (typesResponse.isSuccess() && typesResponse.getDataObject() instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> rawList = (List<Object>) typesResponse.getDataObject();
            allowedTypes = rawList.stream().map(Object::toString).collect(Collectors.toList());
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            // Validate size
            if (maxSizeBytes > 0 && file.getSize() > maxSizeBytes) {
                errors.add(String.format("File '%s' exceeds the maximum size of %d MB.", file.getOriginalFilename(), maxSizeBytes / (1024 * 1024)));
            }

            // Validate type
            if (!allowedTypes.isEmpty()) {
                String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
                if (extension == null || allowedTypes.stream().noneMatch(ext -> ext.equalsIgnoreCase(extension))) {
                    errors.add(String.format("File type of '%s' is not allowed. Allowed types are: %s.",
                            file.getOriginalFilename(), String.join(", ", allowedTypes)));
                }
            }
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private List<FileInfo> processFiles(MultipartFile[] files, String fileType) {
        List<FileInfo> fileInfos = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    ServiceResponse<FileCreatedDTO> fileResponse = fileService.storeFile(file);
                    if (fileResponse.getAckCode() == ServiceResponse.AckCodeType.SUCCESS && fileResponse.getDataObject() != null) {
                        FileInfo fileInfo = fileInfoMapper.toEntity(fileResponse.getDataObject());

                        genericDAO.persist(fileInfo);

                        fileInfos.add(fileInfo);
                        logger.info("Stored {} '{}' for comment.", fileType, fileInfo.getOriginalFilename());
                    } else {
                        // Log and continue, not adding the failed file.
                        logger.warn("Failed to store {} file: {}. Reason: {}",
                                fileType, file.getOriginalFilename(), fileResponse.getMessages());
                    }
                }
            }
        }
        return fileInfos;
    }

    /**
     * Find all discussions associated with the given tags, with their own tags eagerly fetched.
     * @param tagIds A list of tag IDs to filter by.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of discussion summaries for the specified tags.
     */
    @Transactional(readOnly = true)
    public ServiceResponse<PageResponseDTO<DiscussionSummaryDTO>> findPaginatedDiscussionSummariesForTags(
            List<Long> tagIds, Pageable pageable) {

        // If no tags are provided, return an empty page to avoid unnecessary queries.
        if (tagIds == null || tagIds.isEmpty()) {
            return ServiceResponse.success("No tags provided, returning empty result.",
                    PageResponseDTO.from(Page.empty(pageable)));
        }

        try {
            Page<Discussion> discussionPage = discussionRepository.findByTagIdsWithTags(tagIds, pageable);
            Page<DiscussionSummaryDTO> dtoPage = discussionPage.map(discussionMapper::toSummaryDTO);
            return ServiceResponse.success("Fetched Discussion Summaries for tags", PageResponseDTO.from(dtoPage));
        }
        catch (Exception e) {
            logger.error("Error fetching Discussion Summaries for tags: " + tagIds, e);
            return ServiceResponse.failure("An unexpected error occurred while fetching discussions for the selected tags.");
        }
    }

    /**
     * Find all discussions in the system, with tags eagerly fetched.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of discussion summaries.
     */
    @Transactional(readOnly = true)
    public ServiceResponse<PageResponseDTO<DiscussionSummaryDTO>> findPaginatedDiscussionSummaries(Pageable pageable) {
        try {
            Page<Discussion> discussionPage = discussionRepository.findAllWithTags(pageable);
            Page<DiscussionSummaryDTO> dtoPage = discussionPage.map(discussionMapper::toSummaryDTO);
            return ServiceResponse.success("Fetched Discussion Summaries", PageResponseDTO.from(dtoPage));
        }
        catch (Exception e) {
            logger.error("Error fetching Discussion Summaries:", e);
            return ServiceResponse.failure("An unexpected error occurred while fetching discussion summary.");
        }
    }

    /**
     * Find all discussions in a given forum, with tags eagerly fetched.
     * @param forumId The ID of the forum to filter by.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of discussion summaries for the specified forum.
     */
    @Transactional(readOnly = true)
    public ServiceResponse<PageResponseDTO<DiscussionSummaryDTO>> findPaginatedDiscussionSummariesForForum(
            long forumId, Pageable pageable) {

        try {
            Page<Discussion> discussionPage = discussionRepository.findByForumIdWithTags(forumId, pageable);
            Page<DiscussionSummaryDTO> dtoPage = discussionPage.map(discussionMapper::toSummaryDTO);
            return ServiceResponse.success("Fetched Discussion Summaries for forum: %d".formatted(forumId),
                    PageResponseDTO.from(dtoPage));
        }
        catch (Exception e) {
            logger.error("Error fetching Discussion Summaries for forum: " + forumId, e);
            return ServiceResponse.failure("An unexpected error occurred while fetching discussion summary for forum: %d".formatted(forumId));
        }
    }

    @Transactional(readOnly = true)
    public ServiceResponse<PageResponseDTO<DiscussionDTO>> findPaginatedDiscussions(
            long forumId, Pageable pageable) {

        try {
            // Fetch total elements for pagination
            QuerySpec countQuerySpec = QuerySpec.builder(Discussion.class)
                    .filter(FilterSpec.eq("forum.id", forumId))
                    .build();

            long totalElements = dynamicDAO.count(countQuerySpec);

            // Fetch discussions with pagination
            // Assuming page is 1-indexed from the client, convert to 0-indexed for QuerySpec if needed
            // Or adjust QuerySpec to handle 1-indexed page directly.
            int page = pageable.getPageNumber();
            int size = pageable.getPageSize();

            List<OrderSpec> orderSpecs = pageable.getSort().stream().map(
                            order -> order.getDirection().isAscending() ?
                                    OrderSpec.asc(order.getProperty()) : OrderSpec.desc(order.getProperty()))
                    .collect(Collectors.toList());

            QuerySpec querySpec = QuerySpec.builder(Discussion.class)
                    .filter(FilterSpec.eq("forum.id", forumId))
                    .startIndex(page * size).maxResult(size).orders(orderSpecs).build();

            List<Discussion> discussions = dynamicDAO.find(querySpec);

            List<DiscussionDTO> discussionDTOs = discussions.stream()
                    .map(discussionMapper::toDiscussionDTO)
                    .collect(Collectors.toList());

            Page<DiscussionDTO> pageResult = new PageImpl<>(discussionDTOs, pageable, totalElements);

            return ServiceResponse.success("Fetched discussions for forum %d".formatted(forumId),
                    PageResponseDTO.from(pageResult));
        }
        catch (Exception e) {
            logger.error("Error fetching discussions for forum: " + forumId, e);
            return ServiceResponse.failure("An unexpected error occurred while fetching discussions: %d".formatted(forumId));
        }
    }

    /**
     * Retrieves a single discussion by its ID for a detailed view,
     * including its comments.
     *
     * @param discussionId The ID of the discussion to retrieve.
     * @return ServiceResponse containing the DiscussionViewDTO or error details.
     */
    @Transactional(readOnly = true)
    public ServiceResponse<DiscussionDTO> getDiscussion(Long discussionId) {

        if (discussionId == null) {
            logger.warn("Attempted to fetch discussion view with null ID.");
            return ServiceResponse.failure("Discussion ID cannot be null.");
        }

        try {
            Discussion discussion = genericDAO.find(Discussion.class, discussionId);

            if (discussion == null) {
                logger.warn("No discussion found with ID: {}", discussionId);
                return ServiceResponse.failure("Discussion with ID %d not found.".formatted(discussionId));
            }

            // Ensure comments are loaded if they are lazy.
            // If your Discussion.comments mapping is EAGER, this explicit call might not be strictly necessary
            // but doesn't hurt. If it's LAZY, this is crucial.
            // Hibernate.initialize(discussion.getComments()); // Example if using Hibernate directly

            // The DiscussionMapper should handle mapping to DiscussionViewDTO,
            // including mapping the comments list using CommentMapper.
            DiscussionDTO discussionDTO = discussionMapper.toDiscussionDTO(discussion);

            // Publish an event to update view count and last viewed time asynchronously
            eventPublisher.publishEvent(new DiscussionViewedEvent(this, discussion));
            logger.debug("Published DiscussionViewedEvent for discussion ID: {}", discussionId);

            return ServiceResponse.success("Discussion view retrieved successfully.", discussionDTO);
        } catch (Exception e) {
            logger.error(String.format("Error retrieving discussion view for ID %d: ", discussionId), e);
            return ServiceResponse.failure("An unexpected error occurred while retrieving the discussion: %s".formatted(e.getMessage()));
        }
    }

    /**
     * NEW: Finds discussions similar to a given source discussion using a hybrid scoring model.
     * @param sourceDiscussionId The ID of the discussion to find similar items for.
     * @param limit The maximum number of similar discussions to return.
     * @return A ServiceResponse containing a list of DiscussionSummaryDTOs.
     */
    @Transactional(readOnly = true)
    public ServiceResponse<List<DiscussionSummaryDTO>> findSimilarDiscussions(Long sourceDiscussionId, int limit) {
        logger.info("Finding similar discussions for discussion ID: {}", sourceDiscussionId);

        Discussion sourceDiscussion = genericDAO.find(Discussion.class, sourceDiscussionId);
        if (sourceDiscussion == null) {
            return ServiceResponse.failure("Source discussion with ID " + sourceDiscussionId + " not found.");
        }

        SearchSession searchSession = Search.session(entityManager);

        try {
            List<Discussion> similarDiscussions = searchSession.search(Discussion.class)
                    .where(f -> f.bool(b -> {
                        b.mustNot(f.id().matching(sourceDiscussionId));

                        // --- Text similarity replacement ---
                        if (sourceDiscussion.getTitle() != null && !sourceDiscussion.getTitle().isBlank()) {
                            b.should(f.match()
                                    .field("title")
                                    .matching(sourceDiscussion.getTitle())
                                    .boost(2.0f));
                        }
                        if (sourceDiscussion.getContent() != null && !sourceDiscussion.getContent().isBlank()) {
                            b.should(f.match()
                                    .field("content")
                                    .matching(sourceDiscussion.getContent())
                                    .boost(1.5f));
                        }

                        // Forum similarity
                        if (sourceDiscussion.getForum() != null) {
                            b.should(f.match()
                                    .field("forumId")
                                    .matching(sourceDiscussion.getForum().getId())
                                    .boost(3.0f));
                        }

                        // Tag similarity
                        if (sourceDiscussion.getTags() != null) {
                            for (Tag tag : sourceDiscussion.getTags()) {
                                b.should(f.match()
                                        .field("tagIds")
                                        .matching(tag.getId())
                                        .boost(2.5f));
                            }
                        }

                        // Author similarity
                        if (sourceDiscussion.getCreateBy() != null) {
                            b.should(f.match()
                                    .field("createBy")
                                    .matching(sourceDiscussion.getCreateBy())
                                    .boost(1.0f));
                        }
                    }))
                    .sort(f -> f.score().desc())
                    .fetch(limit)
                    .hits();

            List<DiscussionSummaryDTO> dtos = similarDiscussions.stream()
                    .map(discussionMapper::toSummaryDTO)
                    .collect(Collectors.toList());

            return ServiceResponse.success("Successfully found similar discussions.", dtos);

        } catch (Exception e) {
            logger.error("Error finding similar discussions for ID " + sourceDiscussionId, e);
            return ServiceResponse.failure("An unexpected error occurred while finding similar discussions.");
        }
    }

    @Transactional(readOnly = true)
    public ServiceResponse<List<DiscussionDTO>> getLatestDiscussions(int count) {
        try {
            Pageable pageable = PageRequest.of(0, count);
            List<Discussion> discussions = discussionRepository.findByOrderByCreateDateDesc(pageable);
            List<DiscussionDTO> dtos = discussions.stream()
                    .map(discussionMapper::toDiscussionDTO)
                    .collect(Collectors.toList());
            return ServiceResponse.success("Fetched latest discussions.", dtos);
        } catch (Exception e) {
            logger.error("Error fetching latest discussions", e);
            return ServiceResponse.failure("An unexpected error occurred while fetching latest discussions.");
        }
    }

    @Transactional(readOnly = true)
    public ServiceResponse<List<DiscussionDTO>> getMostCommentedDiscussions(int count) {
        try {
            Pageable pageable = PageRequest.of(0, count);
            List<Discussion> discussions = discussionRepository.findByOrderByStatCommentCountDesc(pageable);
            List<DiscussionDTO> dtos = discussions.stream()
                    .map(discussionMapper::toDiscussionDTO)
                    .collect(Collectors.toList());
            return ServiceResponse.success("Fetched most commented discussions.", dtos);
        } catch (Exception e) {
            logger.error("Error fetching most commented discussions", e);
            return ServiceResponse.failure("An unexpected error occurred while fetching most commented discussions.");
        }
    }

    @Transactional(readOnly = true)
    public ServiceResponse<List<DiscussionDTO>> getMostViewedDiscussions(int count) {
        try {
            Pageable pageable = PageRequest.of(0, count);
            List<Discussion> discussions = discussionRepository.findByOrderByStatViewCountDesc(pageable);
            List<DiscussionDTO> dtos = discussions.stream()
                    .map(discussionMapper::toDiscussionDTO)
                    .collect(Collectors.toList());
            return ServiceResponse.success("Fetched most viewed discussions.", dtos);
        } catch (Exception e) {
            logger.error("Error fetching most viewed discussions", e);
            return ServiceResponse.failure("An unexpected error occurred while fetching most viewed discussions.");
        }
    }

    /**
     * Performs a full-text search for discussions.
     *
     * @param keyword  The keyword to search for in discussion titles and content.
     * @param pageable Pagination information.
     * @return A ServiceResponse containing a paginated list of matching DiscussionInfoDTOs.
     */
    @Transactional(readOnly = true)
    public ServiceResponse<PageResponseDTO<DiscussionInfoDTO>> searchDiscussions(String keyword, Pageable pageable) {
        logger.info("Searching discussions with keyword: '{}', pageable: {}", keyword, pageable);

        SearchSession searchSession = Search.session(entityManager);

        try {
            var searchResult = searchSession.search(Discussion.class)
                    .where(f -> f.bool(b -> {
                        // Search in title with a higher weight (boost)
                        b.should(f.match().field("title").boost(2.0f).matching(keyword));
                        // Search in content with normal weight
                        b.should(f.match().field("content").matching(keyword));
                    }))
                    .sort(f -> f.score().then().field("createDate").desc()) // Sort by relevance, then by date
                    .fetch((int) pageable.getOffset(), pageable.getPageSize());

            long totalHits = searchResult.total().hitCount();
            List<Discussion> discussions = searchResult.hits();

            // Manually project to DiscussionInfoDTO, truncating the content
            List<DiscussionInfoDTO> discussionInfoDTOs = discussions.stream()
                    .map(discussion -> new DiscussionInfoDTO(
                            discussion.getId(),
                            discussion.getTitle(),
                            StringUtils.truncate(discussion.getContent(), 255), // Truncate content for summary
                            discussion.getCreateBy(),
                            discussion.getCreateDate()
                    ))
                    .collect(Collectors.toList());

            Page<DiscussionInfoDTO> pageResult = new PageImpl<>(discussionInfoDTOs, pageable, totalHits);

            return ServiceResponse.success("Search successful", PageResponseDTO.from(pageResult));

        } catch (Exception e) {
            logger.error(String.format("Error during discussion search for keyword '%s': ", keyword), e);
            return ServiceResponse.failure("An unexpected error occurred during the search.");
        }
    }

    @Transactional(readOnly = true)
    public ServiceResponse<List<KeywordCountDTO>> getTopTerms(String field, int limit) {

        logger.info("Get discussion top terms for field '{}' with limit '{}'", field, limit);

        // Basic validation for field name to prevent injection-like issues
        if (!"title_terms".equals(field) && !"content_terms".equals(field)) {
            logger.warn("Invalid field name for top terms aggregation: {}", field);
            return ServiceResponse.failure("Invalid field for aggregation.");
        }

        try {
            SearchSession searchSession = Search.session(entityManager);

            // Define a reusable aggregation key
            AggregationKey<Map<String, Long>> topTermsKey = AggregationKey.of("topTerms");

            SearchResult<Discussion> result = searchSession.search(Discussion.class)
                    .where(f -> f.matchAll())
                    .aggregation(topTermsKey, f -> f.terms()
                            .field(field, String.class) // Use the parameter here
                            .orderByCountDescending()
                            .minDocumentCount(1)
                            .maxTermCount(limit) // Use the parameter here
                    )
                    .fetch(0); // No need to fetch hits, just aggregations

            Map<String, Long> topTerms = result.aggregation(topTermsKey);

            return ServiceResponse.success("Successfully get top terms",
                    topTerms.entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                            // The aggregation already limits, but this ensures we don't exceed it
                            .limit(limit)
                            .map(entry -> new KeywordCountDTO(entry.getKey(), entry.getValue()))
                            .collect(Collectors.toList())
            );
        } catch (Exception e) {
            logger.error("Error getting top terms for field '{}': ", field, e);
            return ServiceResponse.failure("An unexpected error occurred while getting top terms.");
        }
    }

    /**
     * Retrieve combined top N terms for multiple fields (e.g. title and content).
     */
    @Transactional(readOnly = true)
    public ServiceResponse<List<KeywordCountDTO>> getTopTermsCombined(List<String> fields, int limit) {

        logger.info("Get discussion top terms combined for fields '{}' with limit '{}'", fields, limit);

        try {
            Map<String, Long> combined = new HashMap<>();

            for (String field : fields) {
                // Append "_terms" to match the index field name
                String aggregationField = field + "_terms";
                ServiceResponse<List<KeywordCountDTO>> termsResponse = getTopTerms(aggregationField, limit);

                if(termsResponse.isSuccess() && termsResponse.getDataObject() != null) {
                    for (KeywordCountDTO entry : termsResponse.getDataObject()) {
                        combined.merge(entry.keyword(), entry.count(), Long::sum);
                    }
                } else {
                    logger.warn("Could not retrieve top terms for field '{}'", aggregationField);
                }
            }

            return ServiceResponse.success("Successfully get top terms combined",
                    combined.entrySet().stream()
                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                            .limit(limit)
                            .map(entry -> new KeywordCountDTO(entry.getKey(), entry.getValue()))
                            .collect(Collectors.toList())
            );
        } catch (Exception e) {
            logger.error("Error getting top terms for fields '{}': ", fields, e);
            return ServiceResponse.failure("An unexpected error occurred while getting top terms combined.");
        }
    }
}