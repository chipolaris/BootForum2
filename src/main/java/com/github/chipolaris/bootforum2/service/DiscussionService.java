package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.*;
import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.domain.DiscussionStat;
import com.github.chipolaris.bootforum2.domain.FileInfo;
import com.github.chipolaris.bootforum2.domain.Forum;
import com.github.chipolaris.bootforum2.dto.*;
import com.github.chipolaris.bootforum2.event.DiscussionCreatedEvent;
import com.github.chipolaris.bootforum2.event.DiscussionViewedEvent;
import com.github.chipolaris.bootforum2.mapper.DiscussionMapper;
import com.github.chipolaris.bootforum2.mapper.FileInfoMapper;
import com.github.chipolaris.bootforum2.repository.DiscussionRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DiscussionService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussionService.class);

    private final EntityManager entityManager;
    private final GenericDAO genericDAO;
    private final DynamicDAO dynamicDAO;
    private final DiscussionRepository discussionRepository;
    private final DiscussionMapper discussionMapper;
    private final FileService fileService;
    private final FileInfoMapper fileInfoMapper; // To map FileInfoDTO from FileStorageService to FileInfo entity
    private final AuthenticationFacade authenticationFacade;
    private final ApplicationEventPublisher eventPublisher;

    // Note: in Spring version >= 4.3, @AutoWired is implied for beans with single constructor
    public DiscussionService(EntityManager entityManager, GenericDAO genericDAO,
                             DynamicDAO dynamicDAO, DiscussionRepository discussionRepository,
                             DiscussionMapper discussionMapper, FileService fileService,
                             FileInfoMapper fileInfoMapper, AuthenticationFacade authenticationFacade,
                             ApplicationEventPublisher eventPublisher) {
        this.entityManager = entityManager;
        this.genericDAO = genericDAO;
        this.dynamicDAO = dynamicDAO;
        this.discussionRepository = discussionRepository;
        this.discussionMapper = discussionMapper;
        this.fileService = fileService;
        this.fileInfoMapper = fileInfoMapper;
        this.authenticationFacade = authenticationFacade;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class, readOnly = false)
    public ServiceResponse<DiscussionDTO> createDiscussion(
            DiscussionCreateDTO discussionCreateDTO,
            MultipartFile[] images,
            MultipartFile[] attachments) {

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

            // 3. Process Files
            List<FileInfo> imageInfos = processFiles(images, "image");
            discussion.setImages(imageInfos);

            List<FileInfo> attachmentInfos = processFiles(attachments, "attachment");
            discussion.setAttachments(attachmentInfos);

            // 4. Initialize Discussion Statistics
            initializeDiscussionStatistics(discussion);

            // 5. Persist Discussion
            genericDAO.persist(discussion);

            // 6. Update Forum Statistics (Candidate for Spring Event)
            //updateForumStatistics(forum, initialComment, username); // Or publish an event here
            logger.info("Successfully created discussion '{}' with ID {}", discussion.getTitle(), discussion.getId());

            eventPublisher.publishEvent(new DiscussionCreatedEvent(this, discussion, username));

            // 6. Map persisted Discussion to DTO for response
            DiscussionDTO discussionDTO = discussionMapper.toDiscussionDTO(discussion);
            return ServiceResponse.success("Discussion created successfully.", discussionDTO);

        } catch (Exception e) {
            logger.error("Error creating discussion: " + discussionCreateDTO.title(), e);
            return ServiceResponse.failure("An unexpected error occurred while creating the discussion: %s".formatted(e.getMessage()));
        }
    }

    private void initializeDiscussionStatistics(Discussion discussion) {
        DiscussionStat discussionStat = discussion.getStat();
        discussionStat.setCommentCount(0);
        discussionStat.setViewCount(0);
        discussionStat.setParticipants(Map.of(discussion.getCreateBy(), 1));

        if (discussion.getImages() != null) {
            discussionStat.setImageCount(discussion.getImages().size());
        }
        if (discussion.getAttachments() != null) {
            discussionStat.setAttachmentCount(discussion.getAttachments().size());
        }
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

    public ServiceResponse<PageResponseDTO<DiscussionSummaryDTO>> findPaginatedDiscussionSummaries(
            long forumId, Pageable pageable) {

        try {
            long discussionCount = discussionRepository.countDiscussionsByForumId(forumId);
            List<DiscussionSummaryDTO> discussionSummaryDTOs = discussionRepository
                    .findDiscussionSummariesByForumId(forumId, pageable);

            Page<DiscussionSummaryDTO> pageResult = new PageImpl<>(discussionSummaryDTOs, pageable, discussionCount);
            return ServiceResponse.success("Fetched Discussion Summaries for forum: %d".formatted(forumId),
                    PageResponseDTO.from(pageResult));
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
     * NEW: Performs a full-text search for discussions.
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
                    //.sort(f -> f.score().then().field("createDate").desc()) // Sort by relevance, then by date
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
}