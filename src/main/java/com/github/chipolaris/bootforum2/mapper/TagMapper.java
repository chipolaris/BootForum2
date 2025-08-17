package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.Tag;
import com.github.chipolaris.bootforum2.dto.TagDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TagMapper {

    @Mapping(target = "discussions", ignore = true) // Don't map the collection
    Tag toEntity(TagDTO tagDTO);

    TagDTO toDTO(Tag tag);
}