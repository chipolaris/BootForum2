package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.Avatar;
import com.github.chipolaris.bootforum2.dto.AvatarDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {FileInfoMapper.class})
public interface AvatarMapper {

    @Mapping(target = "username", source = "userName")
    @Mapping(target = "fileInfo", source = "file")
    AvatarDTO toDTO(Avatar avatar);
}