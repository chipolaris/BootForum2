package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.ForumGroup;
import com.github.chipolaris.bootforum2.dto.ForumGroupCreateDTO;
import com.github.chipolaris.bootforum2.dto.ForumGroupDTO;
import com.github.chipolaris.bootforum2.dto.ForumGroupUpdateDTO;
import com.github.chipolaris.bootforum2.dto.ForumTreeTableDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {ForumMapper.class})
public interface ForumGroupMapper {
    ForumGroup toEntity(ForumGroupDTO dto);
    ForumGroup toEntity(ForumGroupCreateDTO dto);
    @Mapping(target = "id", source = "id")
    ForumGroup toEntity(ForumGroupUpdateDTO dto);
    @Mapping(target = "parentId", source = "parent.id")
    ForumGroupDTO toForumGroupDTO(ForumGroup forumGroup);

    void mergeDTOToEntity(ForumGroupUpdateDTO dto, @MappingTarget ForumGroup entity);

    /**
     * Maps a ForumGroup entity to a ForumTreeTableDTO.
     * The 'forums' list from ForumGroup is mapped to 'forums' in ForumTreeTableDTO.
     * The 'subGroups' list from ForumGroup is mapped to 'forumGroups' in ForumTreeTableDTO.
     *
     * @param forumGroup the source ForumGroup entity
     * @return the mapped ForumTreeTableDTO
     */
    @Mapping(source = "forums", target = "forums")
    @Mapping(source = "subGroups", target = "forumGroups")
    ForumTreeTableDTO toForumTreeTableDTO(ForumGroup forumGroup);
}