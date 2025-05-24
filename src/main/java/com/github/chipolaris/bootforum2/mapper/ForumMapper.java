package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.Forum;
import com.github.chipolaris.bootforum2.dto.ForumCreateDTO;
import com.github.chipolaris.bootforum2.dto.ForumDTO;
import com.github.chipolaris.bootforum2.dto.ForumUpdateDTO;
import com.github.chipolaris.bootforum2.dto.ForumViewDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {ForumGroupMapper.class, DiscussionMapper.class, ForumStatMapper.class})
public interface ForumMapper {

    Forum toEntity(ForumDTO dto);
    Forum toEntity(ForumCreateDTO dto);

    @Mapping(target = "id", source = "id")
    Forum toEntity(ForumUpdateDTO dto);

    @Mapping(target = "forumGroupId", source = "forumGroup.id")
    // The 'stat' field (ForumStat) will be mapped to 'stat' (ForumStatDTO)
    // using ForumStatMapper due to its presence in the 'uses' clause.
    ForumDTO toForumDTO(Forum forum);

    void mergeIntoEntity(ForumUpdateDTO dto, @MappingTarget Forum entity);

    /**
     * Maps a Forum entity to a ForumViewDTO.
     * The 'forumDTO' field is mapped from the input Forum entity.
     * The 'discussionDTOs' field is mapped from the 'discussions' list within the Forum entity.
     *
     * @param forum the source Forum entity
     * @return the mapped ForumViewDTO
     */
    @Mapping(source = "forum", target = "forumDTO")
    @Mapping(source = "discussions", target = "discussionDTOs")
    ForumViewDTO toForumViewDTO(Forum forum);
}
