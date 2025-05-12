package com.github.chipolaris.bootforum2.dto;

import com.github.chipolaris.bootforum2.domain.ForumGroup;

import java.util.List;

public record ForumGroupDTO(Long id, String title, String icon, String iconColor,
                             List<ForumDTO> forums, ForumGroupDTO parent, List<ForumGroupDTO> subGroups) {

    public static ForumGroupDTO fromForumGroup(ForumGroup forumGroup) {
        ForumGroupDTO forumGroupDTO = new ForumGroupDTO(
                forumGroup.getId(),
                forumGroup.getTitle(),
                forumGroup.getIcon(),
                forumGroup.getIconColor(),
                forumGroup.getForums().stream().map(ForumDTO::fromForum).toList(),
                forumGroup.getParent() != null ? fromForumGroup(forumGroup.getParent()) : null,
                forumGroup.getSubGroups().stream().map(ForumGroupDTO::fromForumGroup).toList()
        );

        return forumGroupDTO;
    }
}
