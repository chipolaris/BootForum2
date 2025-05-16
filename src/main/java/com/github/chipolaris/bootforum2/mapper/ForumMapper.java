package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.Forum;
import com.github.chipolaris.bootforum2.dto.ForumCreateDTO;
import com.github.chipolaris.bootforum2.dto.ForumDTO;
import com.github.chipolaris.bootforum2.dto.ForumUpdateDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ForumMapper {

    Forum toEntity(ForumDTO dto);
    Forum toEntity(ForumCreateDTO dto);

    @Mapping(target = "id", source = "id")
    Forum toEntity(ForumUpdateDTO dto);

    ForumDTO toForumDTO(Forum forum);
}
