package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.*;
import com.github.chipolaris.bootforum2.domain.*;
import com.github.chipolaris.bootforum2.dto.DiscussionCreateDTO;
import com.github.chipolaris.bootforum2.dto.DiscussionDTO;
import com.github.chipolaris.bootforum2.dto.FileInfoDTO;
import com.github.chipolaris.bootforum2.dto.PageResponseDTO;
import com.github.chipolaris.bootforum2.mapper.DiscussionMapper;
import com.github.chipolaris.bootforum2.mapper.FileInfoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

import java.util.stream.Collectors;

@Service
public class DiscussionService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussionService.class);

    private final GenericDAO genericDAO;
    private final DynamicDAO dynamicDAO;
    private final DiscussionMapper discussionMapper;
    private final FileStorageService fileStorageService;
    private final FileInfoMapper fileInfoMapper; // To map FileInfoDTO from FileStorageService to FileInfo entity
    private final AuthenticationFacade authenticationFacade;

    public DiscussionService(GenericDAO genericDAO,
                                 DynamicDAO dynamicDAO,
                                 DiscussionMapper discussionMapper,
                                 FileStorageService fileStorageService,
                                 FileInfoMapper fileInfoMapper,
                                 AuthenticationFacade authenticationFacade) {
        this.genericDAO = genericDAO;
        this.dynamicDAO = dynamicDAO;
        this.discussionMapper = discussionMapper;
        this.fileStorageService = fileStorageService;
        this.fileInfoMapper = fileInfoMapper;
        this.authenticationFacade = authenticationFacade;
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

            // 2. Map DTO to Discussion entity
            Discussion discussion = discussionMapper.toEntity(discussionCreateDTO);
            discussion.setForum(forum);
            discussion.setCreateBy(username); // Set creator here

            // 3 & 4. Create Initial Comment and Process Files
            Comment initialComment = createInitialCommentAndProcessFiles(discussion, discussionCreateDTO.comment(), username, images, attachments);
            discussion.setComments(new ArrayList<>(Collections.singletonList(initialComment)));

            // 6. Initialize Discussion Statistics
            DiscussionStat discussionStat = initializeDiscussionStatistics(discussion, initialComment, username);
            discussion.setStat(discussionStat);

            // 7. Persist Discussion
            genericDAO.persist(discussion);

            // Update CommentInfo with persisted comment ID if necessary
            if (initialComment.getId() != null && discussionStat.getLastComment() != null) {
                discussionStat.getLastComment().setCommentId(initialComment.getId());
                // If CommentInfo is an entity and needs to be persisted/merged:
                // genericDAO.merge(discussionStat.getLastComment()); // Or handle via cascade if appropriate
            }

            // 8. Update Forum Statistics (Candidate for Spring Event)
            updateForumStatistics(forum, initialComment, username); // Or publish an event here

            logger.info("Successfully created discussion '{}' with ID {}", discussion.getTitle(), discussion.getId());

            // 9. Map persisted Discussion to DTO for response
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

    private Comment createInitialCommentAndProcessFiles(Discussion discussion, String commentContent, String username, MultipartFile[] images, MultipartFile[] attachments) {
        Comment initialComment = new Comment();
        initialComment.setContent(commentContent);
        initialComment.setDiscussion(discussion);
        initialComment.setCreateBy(username);
        // createDate and updateDate are handled by @PrePersist/@PreUpdate in Comment entity
        initialComment.setReplyTo(null);
        initialComment.setCommentVote(new CommentVote()); // Initialize votes

        List<FileInfo> imageInfos = processFiles(images, initialComment, "image");
        initialComment.setThumbnails(imageInfos); // Assuming 'thumbnails' is the correct field for images

        List<FileInfo> attachmentInfos = processFiles(attachments, initialComment, "attachment");
        initialComment.setAttachments(attachmentInfos);

        return initialComment;
    }

    private DiscussionStat initializeDiscussionStatistics(Discussion discussion, Comment initialComment, String username) {
        DiscussionStat discussionStat = new DiscussionStat();
        discussionStat.setCommentCount(1);
        discussionStat.setViewCount(0);
        discussionStat.setCommentors(Map.of(username, 1)); // Map: (username: commentCount)

        if (initialComment.getThumbnails() != null) {
            discussionStat.setThumbnailCount(initialComment.getThumbnails().size());
        }
        if (initialComment.getAttachments() != null) {
            discussionStat.setAttachmentCount(initialComment.getAttachments().size());
        }

        CommentInfo lastCommentInfo = new CommentInfo();
        // lastCommentInfo.setCommentId(initialComment.getId()); // Set after initialComment is persisted
        lastCommentInfo.setTitle(discussion.getTitle());
        lastCommentInfo.setCommentor(username);
        lastCommentInfo.setCommentDate(initialComment.getCreateDate() != null ? initialComment.getCreateDate() : LocalDateTime.now());
        discussionStat.setLastComment(lastCommentInfo);

        return discussionStat;
    }

    // Method to update forum statistics - can be refactored further or handled by an event listener
    private void updateForumStatistics(Forum forum, Comment initialComment, String username) {
        // Be mindful of potential concurrency issues if not handled carefully.
        // This logic might be better in ForumService or handled by an event listener.
        ForumStat forumStat = forum.getStat();
        // ForumStat should be initialized by @PrePersist in Forum, but defensive check is fine
        if (forumStat == null) {
            logger.warn("ForumStat was null for Forum ID {}. Initializing.", forum.getId());
            forumStat = new ForumStat();
            forum.setStat(forumStat);
        }

        forumStat.addDiscussionCount(1);
        forumStat.addCommentCount(1); // For the initial comment

        CommentInfo forumLastComment = forumStat.getLastComment();
        if (forumLastComment == null) {
            logger.warn("ForumStat.lastComment was null for Forum ID {}. Initializing.", forum.getId());
            forumLastComment = new CommentInfo();
            forumStat.setLastComment(forumLastComment);
        }

        LocalDateTime initialCommentDate = initialComment.getCreateDate() != null ? initialComment.getCreateDate() : LocalDateTime.now();
        if (forumLastComment.getCommentDate() == null || initialCommentDate.isAfter(forumLastComment.getCommentDate())) {
            forumLastComment.setCommentId(initialComment.getId()); // Requires initialComment to have an ID
            forumLastComment.setTitle(initialComment.getDiscussion().getTitle());
            forumLastComment.setCommentor(username);
            forumLastComment.setCommentDate(initialCommentDate);
        }
        genericDAO.merge(forum); // Persist changes to the forum and its stat
    }

    private List<FileInfo> processFiles(MultipartFile[] files, Comment comment, String fileType) {
        List<FileInfo> fileInfos = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    ServiceResponse<FileInfoDTO> fileResponse = fileStorageService.storeFile(file);
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

}