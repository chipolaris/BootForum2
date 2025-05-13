package com.github.chipolaris.bootforum2.dto;

import com.github.chipolaris.bootforum2.domain.Forum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForumDTO(

        Long id, // id can be null

        @NotBlank
        @Size(min = 3, max = 100)
        String title,

        @NotBlank
        @Size(min = 3, max = 500)
        String description,

        @NotBlank
        @Size(max = 50)
        String icon,

        @NotBlank
        @Size(max = 50)
        String iconColor,

        boolean active,

        ForumStatDTO stat) {

        public Forum createForum() {
                Forum forum = new Forum();
                //forum.setId(this.id);
                forum.setTitle(this.title);
                forum.setDescription(this.description);
                forum.setIcon(this.icon);
                forum.setIconColor(this.iconColor);
                forum.setActive(this.active);
                return forum;
        }

        public static ForumDTO fromForum(Forum forum) {
                return forum == null ? null : new ForumDTO(forum.getId(), forum.getTitle(),
                        forum.getDescription(), forum.getIcon(),
                        forum.getIconColor(), forum.isActive(),
                        ForumStatDTO.fromForumStat(forum.getStat()));
        }
}
