package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.*;
import com.github.chipolaris.bootforum2.domain.Comment;
import com.github.chipolaris.bootforum2.domain.CommentVote;
import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.domain.FileInfo;
import com.github.chipolaris.bootforum2.dto.*;
import com.github.chipolaris.bootforum2.event.CommentCreatedEvent;
import com.github.chipolaris.bootforum2.mapper.CommentMapper;
import com.github.chipolaris.bootforum2.mapper.DiscussionMapper;
import com.github.chipolaris.bootforum2.mapper.FileInfoMapper;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);

    private final EntityManager entityManager;
    private final DynamicDAO dynamicDAO;
    private final GenericDAO genericDAO;
    private final CommentMapper commentMapper;
    private final DiscussionMapper discussionMapper;
    private final FileService fileService;
    private final FileInfoMapper fileInfoMapper; // To map FileInfoDTO from FileStorageService to FileInfo entity
    private final AuthenticationFacade authenticationFacade;
    private final ApplicationEventPublisher eventPublisher;
    private final ForumSettingService forumSettingService;

    // Note: in Spring version >= 4.3, @AutoWired is implied for beans with single constructor
    public CommentService(EntityManager entityManager,
                          GenericDAO genericDAO, DynamicDAO dynamicDAO,
                          CommentMapper commentMapper,
                          DiscussionMapper discussionMapper,
                          FileService fileService,
                          FileInfoMapper fileInfoMapper,
                          AuthenticationFacade authenticationFacade,
                          ApplicationEventPublisher eventPublisher,
                          ForumSettingService forumSettingService) {
        this.entityManager = entityManager;
        this.genericDAO = genericDAO;
        this.dynamicDAO = dynamicDAO;
        this.commentMapper = commentMapper;
        this.discussionMapper = discussionMapper;
        this.fileService = fileService;
        this.fileInfoMapper = fileInfoMapper;
        this.authenticationFacade = authenticationFacade;
        this.eventPublisher = eventPublisher;
        this.forumSettingService = forumSettingService;
    }

    @Transactional(readOnly = false)
    public ServiceResponse<CommentDTO> createComment(
            CommentCreateDTO commentCreateDTO,
            MultipartFile[] images,
            MultipartFile[] attachments) {

        // --- START: New Validation Logic ---
        List<String> validationErrors = new ArrayList<>();
        validateContent(commentCreateDTO.content(), validationErrors);
        validateFiles(images, "images", validationErrors);
        validateFiles(attachments, "attachments", validationErrors);

        if (!validationErrors.isEmpty()) {
            logger.warn("Comment creation failed due to validation errors: {}", validationErrors);
            return ServiceResponse.failure(String.join(", ", validationErrors));
        }
        // --- END: New Validation Logic ---

        String username = authenticationFacade.getCurrentUsername().orElse("system");

        try {
            // Fetch Discussion
            Discussion discussion = genericDAO.find(Discussion.class, commentCreateDTO.discussionId());

            if (discussion == null) {
                return ServiceResponse.failure("Discussion not found. Cannot create comment.");
            }

            Comment replyTo = null;

            if(commentCreateDTO.replyToId() != null) {
                replyTo = genericDAO.find(Comment.class, commentCreateDTO.replyToId());

                // check if the replyTo is actually part of the discussion
                if(replyTo != null && !replyTo.getDiscussion().getId().equals(discussion.getId())) {
                    logger.warn("Discussion/replyToId mismatch.");
                    return ServiceResponse.failure("Discussion/replyToId mismatch.");
                }
            }

            // Create Comment entity
            Comment comment = new Comment();
            comment.setTitle(commentCreateDTO.title());
            comment.setContent(commentCreateDTO.content());
            comment.setDiscussion(discussion);
            comment.setReplyTo(replyTo);
            comment.setCreateBy(username); // Set creator here
            comment.setUpdateBy(username); // Set creator here
            comment.setCommentVote(new CommentVote());

            // 3. Process Files
            List<FileInfo> imageInfos = processFiles(images, "image");
            comment.setImages(imageInfos);

            List<FileInfo> attachmentInfos = processFiles(attachments, "attachment");
            comment.setAttachments(attachmentInfos);

            genericDAO.persist(comment);

            logger.info("Successfully created comment '{}' with ID {}", comment.getTitle(), comment.getId());

            eventPublisher.publishEvent(new CommentCreatedEvent(this, comment));

            CommentDTO commentDTO = commentMapper.toCommentDTO(comment);
            return ServiceResponse.success("Comment created successfully.", commentDTO);
        } catch (Exception e) {
            logger.error("Error creating comment: " + commentCreateDTO.title(), e);
            return ServiceResponse.failure("An unexpected error occurred while creating the comment: %s".formatted(e.getMessage()));
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
                    if (fileResponse.isSuccess() && fileResponse.getDataObject() != null) {
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
    public ServiceResponse<PageResponseDTO<CommentDTO>> findPaginatedComments(
            Long discussionId, Pageable pageable) {

        if (discussionId == null) {
            logger.warn("Attempted to fetch comments with null discussionId.");
            return ServiceResponse.failure("Discussion ID cannot be null.");
        }

        try {
            // Count query for total elements
            QuerySpec countQuerySpec = QuerySpec.builder(Comment.class)
                    .filter(FilterSpec.eq("discussion.id", discussionId))
                    .build();
            long totalElements = dynamicDAO.count(countQuerySpec);

            // Data query with pagination and sorting
            int page = pageable.getPageNumber();
            int size = pageable.getPageSize();

            List<OrderSpec> orderSpecs = pageable.getSort().stream()
                    .map(order -> order.getDirection().isAscending() ?
                            OrderSpec.asc(order.getProperty()) : OrderSpec.desc(order.getProperty()))
                    .collect(Collectors.toList());

            // If no sort is provided by pageable, default to createDate ASC (as per controller's @PageableDefault)
            // However, @PageableDefault in controller handles this, so orderSpecs will reflect it.
            // If orderSpecs is empty and a default is strictly needed here, it could be added.
            // For now, relying on Pageable to carry the sort info.

            QuerySpec dataQuerySpec = QuerySpec.builder(Comment.class)
                    .filter(FilterSpec.eq("discussion.id", discussionId))
                    .startIndex(page * size)
                    .maxResult(size)
                    .orders(orderSpecs)
                    .build();

            List<Comment> comments = dynamicDAO.find(dataQuerySpec);

            List<CommentDTO> commentDTOs = comments.stream()
                    .map(commentMapper::toCommentDTO)
                    .collect(Collectors.toList());

            Page<CommentDTO> pageResult = new PageImpl<>(commentDTOs, pageable, totalElements);

            return ServiceResponse.success("Fetched comments for discussion ID: %d".formatted(discussionId),
                    PageResponseDTO.from(pageResult));

        } catch (Exception e) {
            logger.error(String.format("Error fetching comments for discussion ID %d: ", discussionId), e);
            return ServiceResponse.failure("An unexpected error occurred while fetching comments.");
        }
    }

    public ServiceResponse<CommentThreadDTO> getCommentThread(long commentId) {

        Comment comment = genericDAO.find(Comment.class, commentId);
        if (comment == null) {
            logger.error("Comment not found for id {}", commentId);
            return ServiceResponse.failure("Comment not found for id %d".formatted(commentId));
        }

        try {
            DiscussionDTO discussionDTO = discussionMapper.toDiscussionDTO(comment.getDiscussion());
            List<CommentDTO> commentThread = new ArrayList<>();
            Comment currentComment = comment;
            while (currentComment != null) {
                commentThread.add(0, commentMapper.toCommentDTO(currentComment));
                currentComment = currentComment.getReplyTo();
            }

            return ServiceResponse.success("Comment thread retrieved successfully.",
                    new CommentThreadDTO(discussionDTO, commentThread));
        } catch (Exception e) {
            logger.error(String.format("Error fetching comment thread for comment ID %d: ", commentId), e);
            return ServiceResponse.failure("An unexpected error occurred while fetching comments.");
        }
    }

    /**
     * Performs a full-text search for comments based on a keyword.
     * The search is performed on the 'title' and 'content' fields of the Comment entity.
     *
     * @param keyword  The keyword(s) to search for.
     * @param pageable Pagination information.
     * @return A ServiceResponse containing a paginated list of matching CommentDTOs.
     */
    @Transactional(readOnly = true)
    public ServiceResponse<PageResponseDTO<CommentDTO>> searchComments(String keyword, Pageable pageable) {
        logger.info("Searching comments with keyword: '{}'", keyword);

        SearchSession searchSession = Search.session(entityManager);

        try {
            var searchResult = searchSession.search(Comment.class)
                    .where(f -> f.match()
                            .fields("title", "content")
                            .matching(keyword))
                    .fetch((int) pageable.getOffset(), pageable.getPageSize());

            List<Comment> comments = searchResult.hits();
            long totalHits = searchResult.total().hitCount();

            List<CommentDTO> commentDTOs = comments.stream()
                    .map(commentMapper::toCommentDTO)
                    .collect(Collectors.toList());

            Page<CommentDTO> pageResult = new PageImpl<>(commentDTOs, pageable, totalHits);

            return ServiceResponse.success("Search successful", PageResponseDTO.from(pageResult));

        } catch (Exception e) {
            logger.error(String.format("Error during comment search for keyword '%s': ", keyword), e);
            return ServiceResponse.failure("An unexpected error occurred during the search.");
        }
    }
}