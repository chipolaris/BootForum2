package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.CommentVote;
import com.github.chipolaris.bootforum2.dto.CommentVoteDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", uses = {VoteMapper.class}) // Added VoteMapper to 'uses'
public interface CommentVoteMapper {

    // CommentVoteMapper INSTANCE = Mappers.getMapper(CommentVoteMapper.class);

    CommentVoteDTO toDTO(CommentVote commentVote);

    CommentVote toEntity(CommentVoteDTO commentVoteDTO);

    /**
     * Updates an existing CommentVote entity from a CommentVoteDTO.
     * The ID is typically not updated from the DTO.
     * Collections like 'votes' might need careful handling for updates
     * (e.g., clearing and re-adding, or more granular updates if needed).
     * By default, MapStruct will attempt to map the collection.
     * @param commentVoteDTO the source DTO
     * @param commentVote the target entity to update
     */
    void updateEntityFromDto(CommentVoteDTO commentVoteDTO, @MappingTarget CommentVote commentVote);
}