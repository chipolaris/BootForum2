package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.dto.DiscussionCreateDTO;
import com.github.chipolaris.bootforum2.dto.DiscussionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {FileInfoMapper.class, TagMapper.class, DiscussionStatMapper.class})
public interface DiscussionMapper {

    @Mapping(source = "stat", target = "stat")
    DiscussionDTO toDiscussionDTO(Discussion discussion);

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