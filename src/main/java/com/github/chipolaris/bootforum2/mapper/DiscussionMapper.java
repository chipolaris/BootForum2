package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.dto.DiscussionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {TagMapper.class, DiscussionStatMapper.class})
public interface DiscussionMapper {

    @Mapping(source = "stat", target = "stat")
    DiscussionDTO toDTO(Discussion discussion);
}