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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DiscussionService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussionService.class);

    private final GenericDAO genericDAO;
    private final DynamicDAO dynamicDAO;
    private final DiscussionRepository discussionRepository;
    private final DiscussionMapper discussionMapper;
    private final FileStorageService fileStorageService;
    private final FileInfoMapper fileInfoMapper; // To map FileInfoDTO from FileStorageService to FileInfo entity
    private final AuthenticationFacade authenticationFacade;
    private final ApplicationEventPublisher eventPublisher;

    // Note: in Spring version >= 4.3, @AutoWired is implied for beans with single constructor
    public DiscussionService(GenericDAO genericDAO,
                                 DynamicDAO dynamicDAO,
                                 DiscussionRepository discussionRepository,
                                 DiscussionMapper discussionMapper,
                                 FileStorageService fileStorageService,
                                 FileInfoMapper fileInfoMapper,
                                 AuthenticationFacade authenticationFacade,
                                 ApplicationEventPublisher eventPublisher) {
        this.genericDAO = genericDAO;
        this.dynamicDAO = dynamicDAO;
        this.discussionRepository = discussionRepository;
        this.discussionMapper = discussionMapper;
        this.fileStorageService = fileStorageService;
        this.fileInfoMapper = fileInfoMapper;
        this.authenticationFacade = authenticationFacade;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class, readOnly = false)
    public ServiceResponse<DiscussionDTO> createDiscussion(
            DiscussionCreateDTO discussionCreateDTO,
            MultipartFile[] images,
            MultipartFile[] attachments) {

        ServiceResponse<DiscussionDTO> response = new ServiceResponse<>();
        String username = authenticationFacade.getCurrentUsername().orElse("system");

        try {
            // 1. Fetch Forum
            Forum forum = genericDAO.find(Forum.class, discussionCreateDTO.forumId());
            if (forum == null) {
                // ... error handling ...
                return response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                        .addMessage("Forum not found. Cannot create discussion.");
            }

            // 2. create Discussion entity
            Discussion discussion = Discussion.newDiscussion();
            discussion.setTitle(discussionCreateDTO.title());
            discussion.setContent(discussionCreateDTO.content());
            discussion.setForum(forum);
            discussion.setCreateBy(username); // Set creator here

            // 3. Process Files
            List<FileInfo> imageInfos = processFiles(images, "image");
            discussion.setThumbnails(imageInfos);

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
            response.setDataObject(discussionDTO);
            response.addMessage("Discussion created successfully.");

        } catch (Exception e) {
            logger.error("Error creating discussion: " + discussionCreateDTO.title(), e);
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE);
            response.addMessage("An unexpected error occurred while creating the discussion: " + e.getMessage());
        }
        return response;
    }

    private void initializeDiscussionStatistics(Discussion discussion) {
        DiscussionStat discussionStat = discussion.getStat();
        discussionStat.setCommentCount(0);
        discussionStat.setViewCount(0);
        discussionStat.setParticipants(Map.of(discussion.getCreateBy(), 1));

        if (discussion.getThumbnails() != null) {
            discussionStat.setThumbnailCount(discussion.getThumbnails().size());
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
                    ServiceResponse<FileCreatedDTO> fileResponse = fileStorageService.storeFile(file);
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

        ServiceResponse<PageResponseDTO<DiscussionSummaryDTO>> response = new ServiceResponse<>();

        try {

            long discussionCount = discussionRepository.countDiscussionsByForumId(forumId);

            List<DiscussionSummaryDTO> discussionSummaryDTOs = discussionRepository
                    .findDiscussionSummariesByForumId(forumId, pageable);

            Page<DiscussionSummaryDTO> pageResult = new PageImpl<>(discussionSummaryDTOs, pageable, discussionCount);

            response.setDataObject(PageResponseDTO.from(pageResult))
                    .addMessage("Fetched Discussion Summaries for forum: " + forumId);
        }
        catch (Exception e) {
            logger.error("Error fetching Discussion Summaries for forum: " + forumId, e);
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage("An unexpected error occurred while fetching discussion summary for forum: " + forumId);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public ServiceResponse<PageResponseDTO<DiscussionDTO>> findPaginatedDiscussions(
            long forumId, Pageable pageable) {

        ServiceResponse<PageResponseDTO<DiscussionDTO>> response = new ServiceResponse<>();

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

            response.setDataObject(PageResponseDTO.from(pageResult))
                    .addMessage("Fetched discussions for forum: " + forumId);
        }
        catch (Exception e) {
            logger.error("Error fetching discussions for forum: " + forumId, e);
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                .addMessage("An unexpected error occurred while fetching discussions for forum: " + forumId);
        }

        return response;
    }

    /**
     * Retrieves a single discussion by its ID for a detailed view,
     * including its comments.
     *
     * @param discussionId The ID of the discussion to retrieve.
     * @return ServiceResponse containing the DiscussionViewDTO or error details.
     */
    @Transactional(readOnly = true)
    public ServiceResponse<DiscussionDTO> findDiscussion(Long discussionId) {
        ServiceResponse<DiscussionDTO> response = new ServiceResponse<>();

        if (discussionId == null) {
            logger.warn("Attempted to fetch discussion view with null ID.");
            return response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage("Discussion ID cannot be null.");
        }

        try {
            Discussion discussion = genericDAO.find(Discussion.class, discussionId);

            if (discussion == null) {
                logger.warn("No discussion found with ID: {}", discussionId);
                return response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                        .addMessage(String.format("Discussion with ID %d not found.", discussionId));
            }

            // Ensure comments are loaded if they are lazy.
            // If your Discussion.comments mapping is EAGER, this explicit call might not be strictly necessary
            // but doesn't hurt. If it's LAZY, this is crucial.
            // Hibernate.initialize(discussion.getComments()); // Example if using Hibernate directly

            // The DiscussionMapper should handle mapping to DiscussionViewDTO,
            // including mapping the comments list using CommentMapper.
            DiscussionDTO discussionDTO = discussionMapper.toDiscussionDTO(discussion);
            response.setDataObject(discussionDTO);
            response.addMessage("Discussion view retrieved successfully.");

            // Publish an event to update view count and last viewed time asynchronously
            eventPublisher.publishEvent(new DiscussionViewedEvent(this, discussion));
            logger.debug("Published DiscussionViewedEvent for discussion ID: {}", discussionId);

        } catch (Exception e) {
            logger.error(String.format("Error retrieving discussion view for ID %d: ", discussionId), e);
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                    .addMessage("An unexpected error occurred while retrieving the discussion: " + e.getMessage());
        }

        return response;
    }
}