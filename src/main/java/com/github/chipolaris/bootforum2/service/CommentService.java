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
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
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

    // Note: in Spring version >= 4.3, @AutoWired is implied for beans with single constructor
    public CommentService(EntityManager entityManager,
                          GenericDAO genericDAO, DynamicDAO dynamicDAO,
                          CommentMapper commentMapper,
                          DiscussionMapper discussionMapper,
                          FileService fileService,
                          FileInfoMapper fileInfoMapper,
                          AuthenticationFacade authenticationFacade,
                          ApplicationEventPublisher eventPublisher) {
        this.entityManager = entityManager;
        this.genericDAO = genericDAO;
        this.dynamicDAO = dynamicDAO;
        this.commentMapper = commentMapper;
        this.discussionMapper = discussionMapper;
        this.fileService = fileService;
        this.fileInfoMapper = fileInfoMapper;
        this.authenticationFacade = authenticationFacade;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = false)
    public ServiceResponse<CommentDTO> createComment(
            CommentCreateDTO commentCreateDTO,
            MultipartFile[] images,
            MultipartFile[] attachments) {

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
                if(replyTo != null && replyTo.getDiscussion().getId() != discussion.getId()) {
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