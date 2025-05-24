package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.CommentInfo;
import com.github.chipolaris.bootforum2.dto.CommentInfoDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommentInfoMapper {

    CommentInfoDTO toDTO(CommentInfo commentInfo);
}