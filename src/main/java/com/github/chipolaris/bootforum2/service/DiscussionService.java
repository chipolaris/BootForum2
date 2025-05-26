package com.github.chipolaris.bootforum2.service;

import com.github.chipolaris.bootforum2.dao.*;
import com.github.chipolaris.bootforum2.domain.*;
import com.github.chipolaris.bootforum2.dto.DiscussionCreateDTO;
import com.github.chipolaris.bootforum2.dto.DiscussionDTO;
import com.github.chipolaris.bootforum2.dto.FileInfoDTO;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.Optional;
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

    @Transactional
    public ServiceResponse<DiscussionDTO> createDiscussion(
            DiscussionCreateDTO discussionCreateDTO,
            MultipartFile[] images,
            MultipartFile[] attachments) {

        ServiceResponse<DiscussionDTO> response = new ServiceResponse<>();

        Optional<String> currentUsernameOpt = authenticationFacade.getCurrentUsername();
        // Use a default or handle if no user is found, though for creating discussions, a user should typically be logged in.
        String username = currentUsernameOpt.orElse("system"); // Or throw an exception if user must be present

        try {
            // 1. Fetch Forum
            Forum forum = genericDAO.find(Forum.class, discussionCreateDTO.forumId());
            if (forum == null) {
                logger.warn("Forum with ID {} not found.", discussionCreateDTO.forumId());
                return response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                        .addMessage("Forum not found. Cannot create discussion.");
            }

            // 2. Map DTO to Discussion entity (partially)
            Discussion discussion = discussionMapper.toEntity(discussionCreateDTO);
            discussion.setForum(forum);
            // createDate and updateDate are handled by @PrePersist in Discussion entity

            // 3. Create the initial Comment
            Comment initialComment = new Comment();
            initialComment.setContent(discussionCreateDTO.comment());
            initialComment.setDiscussion(discussion);
            initialComment.setCreateBy(username);
            initialComment.setCreateDate(LocalDateTime.now());
            initialComment.setUpdateDate(LocalDateTime.now());
            initialComment.setReplyTo(null); // First comment doesn't reply to anything
            // TODO: fix this
            //// initialComment.setDeleted(false);

            // 4. Handle file uploads for the initial comment
            List<FileInfo> imageInfos = processFiles(images, initialComment, "image");
            // TODO: fix this
            //// initialComment.setImages(imageInfos);

            List<FileInfo> attachmentInfos = processFiles(attachments, initialComment, "attachment");
            initialComment.setAttachments(attachmentInfos);

            // 5. Add initial comment to discussion's comment list
            discussion.setComments(new ArrayList<>(Collections.singletonList(initialComment)));

            // 6. Create DiscussionStat
            DiscussionStat discussionStat = new DiscussionStat();
            discussionStat.setCommentCount(1); // Initial comment
            discussionStat.setViewCount(0); // Initial view count
            discussionStat.setThumbnailCount(imageInfos.size());
            discussionStat.setAttachmentCount(attachmentInfos.size());

            // Create CommentInfo for lastComment in DiscussionStat
            CommentInfo lastCommentInfo = new CommentInfo();
            lastCommentInfo.setCommentId(null); // Will be set after comment is persisted if needed, or keep null
            lastCommentInfo.setTitle(discussion.getTitle()); // Or some snippet of comment
            lastCommentInfo.setCommentor(username); // Or a display name
            lastCommentInfo.setCommentDate(initialComment.getCreateDate());
            discussionStat.setLastComment(lastCommentInfo);

            // Add current user to commentors map (if tracking)
            // discussionStat.getCommentors().put(initialComment.getCreateUser(), 1);

            discussion.setStat(discussionStat);

            // 7. Persist Discussion (CascadeType.ALL should handle Comment and DiscussionStat)
            genericDAO.persist(discussion);
            // At this point, discussion, initialComment, and discussionStat should have IDs.
            // If you need the comment ID for CommentInfo:
            if (initialComment.getId() != null) {
                discussionStat.getLastComment().setCommentId(initialComment.getId());
                // If CommentInfo is an entity and needs to be persisted/merged:
                // genericDAO.merge(discussionStat.getLastComment());
            }


            // 8. Update Forum statistics (this might be better handled by an event/listener or a separate scheduled task for accuracy at scale)
            // For simplicity, updating directly here.
            // Be mindful of potential concurrency issues if not handled carefully.
            ForumStat forumStat = forum.getStat();
            if (forumStat == null) { // Should not happen due to @PrePersist in Forum
                forumStat = new ForumStat();
                forum.setStat(forumStat);
            }
            forumStat.setDiscussionCount(forumStat.getDiscussionCount() + 1);
            forumStat.setCommentCount(forumStat.getCommentCount() + 1); // For the initial comment

            // Update last comment in forumStat
            CommentInfo forumLastComment = forumStat.getLastComment();
            if (forumLastComment == null) { // Should not happen due to @PrePersist in Forum
                forumLastComment = new CommentInfo();
                forumStat.setLastComment(forumLastComment);
            }
            // Assuming initialComment.getCreateDate() is more recent or if it's the first
            if (forumLastComment.getCommentDate() == null || initialComment.getCreateDate().isAfter(forumLastComment.getCommentDate())) {
                forumLastComment.setCommentId(initialComment.getId());
                forumLastComment.setTitle(discussion.getTitle()); // Or a snippet
                forumLastComment.setCommentor(username);
                forumLastComment.setCommentDate(initialComment.getCreateDate());
            }
            genericDAO.merge(forum);


            logger.info("Successfully created discussion '{}' with ID {}", discussion.getTitle(), discussion.getId());

            // 9. Map persisted Discussion to DTO for response
            DiscussionDTO discussionDTO = discussionMapper.toDiscussionDTO(discussion);
            response.setDataObject(discussionDTO);
            response.addMessage("Discussion created successfully.");

        } catch (Exception e) {
            logger.error("Error creating discussion: " + discussionCreateDTO.title(), e);
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE);
            response.addMessage("An unexpected error occurred while creating the discussion: " + e.getMessage());
            // Consider re-throwing a custom exception if you have global exception handling
            // that can translate it to a proper HTTP response.
        }

        return response;
    }

    private List<FileInfo> processFiles(MultipartFile[] files, Comment comment, String fileType) {
        List<FileInfo> fileInfos = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    ServiceResponse<FileInfoDTO> fileResponse = fileStorageService.storeFile(file);
                    if (fileResponse.getAckCode() == ServiceResponse.AckCodeType.SUCCESS && fileResponse.getDataObject() != null) {
                        FileInfo fileInfo = fileInfoMapper.toEntity(fileResponse.getDataObject());
                        // fileInfo.setComment(comment); // If FileInfo has a direct back-reference to Comment
                        fileInfos.add(fileInfo);
                        logger.info("Stored {} '{}' for comment.", fileType, fileInfo.getOriginalFilename());
                    } else {
                        logger.warn("Failed to store {} file: {}. Reason: {}",
                                fileType, file.getOriginalFilename(), fileResponse.getMessages());
                        // Decide if this should be a critical failure for the whole discussion creation
                        // For now, we'll log and continue, not adding the failed file.
                    }
                }
            }
        }
        return fileInfos;
    }

    public ServiceResponse<Page<DiscussionDTO>> findPaginatedDiscussions(
            long forumId, Pageable pageable) {

        ServiceResponse<Page<DiscussionDTO>> response = new ServiceResponse<>();

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
            int startIndex = page * size;

            QuerySpec querySpec = QuerySpec.builder(Discussion.class)
                    .filter(FilterSpec.eq("forum.id", forumId))
                    .startIndex(startIndex).maxResult(size)
                    .order(OrderSpec.desc("stat.lastComment.commentDate"))
                    .build();

            List<Discussion> discussions = dynamicDAO.find(querySpec);

            List<DiscussionDTO> discussionDTOs = discussions.stream()
                    .map(discussionMapper::toDiscussionDTO)
                    .collect(Collectors.toList());

            Page<DiscussionDTO> pageResult = new PageImpl<>(discussionDTOs, pageable, totalElements);

            response.setDataObject(pageResult).addMessage("Fetched discussions for forum: " + forumId);
        }
        catch (Exception e) {
            logger.error("Error fetching discussions for forum: " + forumId, e);
            response.setAckCode(ServiceResponse.AckCodeType.FAILURE)
                .addMessage("An unexpected error occurred while fetching discussions for forum: " + forumId);
        }

        return response;
    }

}