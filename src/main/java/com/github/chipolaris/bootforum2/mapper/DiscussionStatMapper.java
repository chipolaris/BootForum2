package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.DiscussionStat;
import com.github.chipolaris.bootforum2.dto.DiscussionStatDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CommentInfoMapper.class}) // Ensure CommentInfoMapper is used
public interface DiscussionStatMapper {

    // The 'lastComment' field will now be mapped using CommentInfoMapper directly
    // from DiscussionStat.lastComment (CommentInfo) to DiscussionStatDTO.lastComment (CommentInfoDTO)
    // Other fields like commentCount, viewCount, etc., will be mapped by convention.
    @Mapping(source = "lastComment", target = "lastComment")
    DiscussionStatDTO toDTO(DiscussionStat discussionStat);
}