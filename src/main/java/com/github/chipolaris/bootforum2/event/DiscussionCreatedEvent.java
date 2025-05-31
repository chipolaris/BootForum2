package com.github.chipolaris.bootforum2.event;

import com.github.chipolaris.bootforum2.domain.Discussion;
import org.springframework.context.ApplicationEvent;

public class DiscussionCreatedEvent extends ApplicationEvent {
    private final Discussion discussion;
    private final String creatorUsername;

    public DiscussionCreatedEvent(Object source, Discussion discussion, String creatorUsername) {
        super(source);
        this.discussion = discussion;
        this.creatorUsername = creatorUsername;
    }

    public Discussion getDiscussion() {
        return discussion;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }
}