package com.github.chipolaris.bootforum2.dto;

import com.github.chipolaris.bootforum2.domain.ForumGroup;

import java.util.ArrayList;
import java.util.List;

public record ForumGroupDTO(Long id, String title, String icon, String iconColor, Long parentId,
                             List<ForumDTO> forums, List<ForumGroupDTO> subGroups) {

    public static ForumGroupDTO fromForumGroup(ForumGroup forumGroup) {
        ForumGroupDTO forumGroupDTO = new ForumGroupDTO(
                forumGroup.getId(),
                forumGroup.getTitle(),
                forumGroup.getIcon(),
                forumGroup.getIconColor(),
                forumGroup.getParent() != null ? forumGroup.getParent().getId() : null,
                forumGroup.getForums() != null ? forumGroup.getForums().stream().map(ForumDTO::fromForum).toList() : null,
                // forumGroup.getParent() != null ? fromForumGroup(forumGroup.getParent()) : null,
                forumGroup.getSubGroups() != null ? forumGroup.getSubGroups().stream().map(ForumGroupDTO::fromForumGroup).toList() : null
        );

        return forumGroupDTO;
    }

    public ForumGroup createForumGroup() {
        ForumGroup forumGroup = new ForumGroup();
        forumGroup.setTitle(this.title);
        forumGroup.setIcon(this.icon);
        forumGroup.setIconColor(this.iconColor);

        return forumGroup;
    }
}
