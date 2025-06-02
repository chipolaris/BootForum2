package com.github.chipolaris.bootforum2.event;

import com.github.chipolaris.bootforum2.domain.Discussion;
import org.springframework.context.ApplicationEvent;

public class DiscussionViewedEvent extends ApplicationEvent {

    private final Discussion discussion;

    public DiscussionViewedEvent(Object source, Discussion discussion) {
        super(source);
        this.discussion = discussion;
    }

    public Discussion getDiscussion() {
        return discussion;
    }
}