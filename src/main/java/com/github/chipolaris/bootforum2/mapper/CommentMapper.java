package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.Comment;
import com.github.chipolaris.bootforum2.dto.CommentDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {FileInfoMapper.class, CommentVoteMapper.class})
public interface CommentMapper {

    // Don't need this field as this mapper will be used in the Spring context (injected)
    // CommentMapper INSTANCE = Mappers.getMapper(CommentMapper.class);

    // BaseEntity fields are mapped by name if not specified.
    // For clarity, they can be explicitly mapped:
    @Mapping(target = "createDate", source = "createDate")
    @Mapping(target = "createBy", source = "createBy")
    @Mapping(target = "updateDate", source = "updateDate")
    @Mapping(target = "updateBy", source = "updateBy")
    // 'discussion' is ignored by not having a target field in CommentDTO
    // and not being explicitly mapped.
    // 'replies' will be mapped recursively by MapStruct.
    // 'attachments', 'thumbnails', 'commentVote' will use their respective mappers.
    @Mapping(target = "replyToId", source = "replyTo.id")
    CommentDTO toCommentDTO(Comment comment);

    @Mapping(target = "createDate", ignore = true) // Typically set by @PrePersist
    @Mapping(target = "createBy", ignore = true)   // Typically set by system/security context
    @Mapping(target = "updateDate", ignore = true) // Typically set by @PreUpdate
    @Mapping(target = "updateBy", ignore = true)   // Typically set by system/security context
    @Mapping(target = "discussion", ignore = true) // Explicitly ignore for toEntity
    Comment toEntity(CommentDTO commentDTO);

    List<CommentDTO> toCommentDTOs(List<Comment> comments);

    List<Comment> toEntities(List<CommentDTO> commentDTOs);
}