package com.github.chipolaris.bootforum2.mapper;

import com.github.chipolaris.bootforum2.domain.Comment;
import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.dto.UserProfileCommentDTO;
import com.github.chipolaris.bootforum2.dto.UserProfileDTO;
import com.github.chipolaris.bootforum2.dto.UserProfileDiscussionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.person.firstName", target = "firstName")
    @Mapping(source = "user.person.lastName", target = "lastName")
    @Mapping(source = "user.createDate", target = "joinDate")
    @Mapping(source = "user.stat.discussionCount", target = "discussionCreatedCount")
    @Mapping(source = "user.stat.commentCount", target = "commentCount")
    @Mapping(source = "user.stat.thumbnailCount", target = "imageUploaded") // Assuming thumbnails are images
    @Mapping(source = "user.stat.attachmentCount", target = "attachmentUploaded")
    @Mapping(source = "user.stat.reputation", target = "reputation")
    @Mapping(source = "user.stat.profileViewed", target = "profileViewed")
    @Mapping(source = "user.stat.lastLogin", target = "lastLogin")
    @Mapping(source = "discussions", target = "discussions")
    @Mapping(source = "comments", target = "comments")
    UserProfileDTO toUserProfileDTO(User user, List<UserProfileDiscussionDTO> discussions, List<UserProfileCommentDTO> comments);

    @Mapping(source = "id", target = "discussionId")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "createDate", target = "createdDate")
    @Mapping(source = "content", target = "content", qualifiedByName = "truncateContent")
    // The 'discussionTitle' field in the DTO seems redundant with 'title', so we map 'title' to both for now.
    // If it's meant to be something else, adjust the source.
    @Mapping(source = "title", target = "discussionTitle")
    UserProfileDiscussionDTO discussionToUserProfileDiscussionDTO(Discussion discussion);

    @Mapping(source = "id", target = "commentId")
    @Mapping(source = "createDate", target = "createdDate")
    @Mapping(source = "title", target = "commentTitle")
    @Mapping(source = "content", target = "content", qualifiedByName = "truncateContent")
    @Mapping(source = "discussion.id", target = "discussionId")
    @Mapping(source = "discussion.title", target = "discussionTitle")
    UserProfileCommentDTO commentToUserProfileCommentDTO(Comment comment);

    @Named("truncateContent")
    default String truncateContent(String content) {
        if (content == null) {
            return null;
        }
        return content.length() > 200 ? content.substring(0, 200) + "..." : content;
    }
}