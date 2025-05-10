package com.github.chipolaris.bootforum2.dto;

import com.github.chipolaris.bootforum2.domain.Forum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForumDataPayload(
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

        boolean active) {

        public Forum toForum() {
                Forum forum = new Forum();
                forum.setTitle(this.title);
                forum.setDescription(this.description);
                forum.setIcon(this.icon);
                forum.setIconColor(this.iconColor);
                forum.setActive(this.active);
                return forum;
        }
}
