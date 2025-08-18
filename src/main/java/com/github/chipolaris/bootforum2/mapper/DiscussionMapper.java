package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.dto.DiscussionCreateDTO;
import com.github.chipolaris.bootforum2.dto.DiscussionDTO;
import com.github.chipolaris.bootforum2.dto.DiscussionSummaryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {FileInfoMapper.class, TagMapper.class, DiscussionStatMapper.class})
public interface DiscussionMapper {

    @Mapping(source = "stat", target = "stat")
    @Mapping(source = "forum.id", target = "forumId")
    @Mapping(source = "forum.title", target = "forumTitle")
    DiscussionDTO toDiscussionDTO(Discussion discussion);

    /**
     * Maps a Discussion entity to a DiscussionSummaryDTO.
     * This is used for list views and is more lightweight than the full DiscussionDTO.
     * @param discussion The source Discussion entity.
     * @return A DiscussionSummaryDTO.
     */
    @Mapping(source = "stat.commentCount", target = "commentCount")
    @Mapping(source = "stat.viewCount", target = "viewCount")
    @Mapping(source = "stat.lastComment.commentDate", target = "lastCommentDate")
    @Mapping(source = "forum.id", target = "forumId")
    @Mapping(source = "forum.title", target = "forumTitle")
    @Mapping(source = "tags", target = "tags")
    DiscussionSummaryDTO toSummaryDTO(Discussion discussion);

    /**
     * Maps a DiscussionCreateDTO to a Discussion entity.
     * This mapping focuses on direct field transfers.
     * - forumId from the DTO is used by the service to fetch and set the Forum.
     * - Default values are set for boolean flags.
     * - Collections and related entities (comments, stat, forum, tags) are ignored here
     *   as they require more complex logic typically handled in the service layer.
     *
     * @param discussionCreateDTO The source DTO.
     * @return A partially populated Discussion entity.
     */
    @Mapping(target = "id", ignore = true) // ID is auto-generated
    @Mapping(target = "closed", constant = "false") // Default for new discussions
    @Mapping(target = "sticky", constant = "false") // Default for new discussions
    @Mapping(target = "important", constant = "false") // Default for new discussions
    @Mapping(target = "comments", ignore = true) // Initial comments will be empty
    @Mapping(target = "stat", ignore = true) // DiscussionStat created in service
    @Mapping(target = "forum", ignore = true) // Forum set in service using forumId from DTO
    @Mapping(target = "tags", ignore = true) // Tags are not part of the initial creation DTO
    @Mapping(target = "createDate", ignore = true) // Handled by @PrePersist
    @Mapping(target = "updateDate", ignore = true) // Handled by @PrePersist
    Discussion toEntity(DiscussionCreateDTO discussionCreateDTO);
}