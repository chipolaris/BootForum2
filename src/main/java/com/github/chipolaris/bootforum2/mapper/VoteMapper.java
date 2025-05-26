package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.Vote;
import com.github.chipolaris.bootforum2.dto.VoteDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring") // Assuming Vote entity has fields like voter, voteDate, voteType
public interface VoteMapper {

    // VoteMapper INSTANCE = Mappers.getMapper(VoteMapper.class);

    VoteDTO toDTO(Vote vote);

    Vote toEntity(VoteDTO voteDTO);

    void updateEntityFromDto(VoteDTO voteDTO, @MappingTarget Vote vote);
}