package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.FileInfo;
import com.github.chipolaris.bootforum2.dto.FileInfoDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface FileInfoMapper {

    //FileInfoMapper INSTANCE = Mappers.getMapper(FileInfoMapper.class);

    FileInfoDTO toDTO(FileInfo fileInfo);

    FileInfo toEntity(FileInfoDTO fileInfoDTO);

    /**
     * Updates an existing FileInfo entity from a FileInfoDTO.
     * The ID is typically not updated from the DTO.
     * @param fileInfoDTO the source DTO
     * @param fileInfo the target entity to update
     */
    void updateEntityFromDto(FileInfoDTO fileInfoDTO, @MappingTarget FileInfo fileInfo);
}