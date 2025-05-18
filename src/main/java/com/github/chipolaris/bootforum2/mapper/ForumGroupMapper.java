package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.ForumGroup;
import com.github.chipolaris.bootforum2.dto.ForumGroupCreateDTO;
import com.github.chipolaris.bootforum2.dto.ForumGroupDTO;
import com.github.chipolaris.bootforum2.dto.ForumGroupUpdateDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ForumGroupMapper {
    ForumGroup toEntity(ForumGroupDTO dto);
    ForumGroup toEntity(ForumGroupCreateDTO dto);
    @Mapping(target = "id", source = "id")
    ForumGroup toEntity(ForumGroupUpdateDTO dto);

    ForumGroupDTO toForumGroupDTO(ForumGroup forumGroup);

    void mergeDTOToEntity(ForumGroupUpdateDTO dto, @MappingTarget ForumGroup entity);
}
