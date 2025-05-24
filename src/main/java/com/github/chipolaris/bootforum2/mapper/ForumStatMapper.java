package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.ForumStat;
import com.github.chipolaris.bootforum2.dto.ForumStatDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CommentInfoMapper.class})
public interface ForumStatMapper {

    @Mapping(source = "lastComment", target = "lastComment") // MapStruct will use CommentInfoMapper for this
    ForumStatDTO toDTO(ForumStat forumStat);
}