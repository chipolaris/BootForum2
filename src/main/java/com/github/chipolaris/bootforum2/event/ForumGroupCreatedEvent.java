package com.github.chipolaris.bootforum2.event;

import com.github.chipolaris.bootforum2.domain.ForumGroup;
import org.springframework.context.ApplicationEvent;

public class ForumGroupCreatedEvent extends ApplicationEvent {
    private final ForumGroup forumGroup;

    public ForumGroupCreatedEvent(Object source, ForumGroup forumGroup) {
        super(source);
        this.forumGroup = forumGroup;
    }

    public ForumGroup getForumGroup() {
        return forumGroup;
    }
}
