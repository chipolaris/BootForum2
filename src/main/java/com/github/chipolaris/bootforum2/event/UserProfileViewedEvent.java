package com.github.chipolaris.bootforum2.event;

import com.github.chipolaris.bootforum2.domain.Comment;
import org.springframework.context.ApplicationEvent;

public class UserProfileViewedEvent extends ApplicationEvent {

    private final String viewedUsername;

    public UserProfileViewedEvent(Object source, String viewedUsername) {
        super(source);
        this.viewedUsername = viewedUsername;
    }

    public String getViewedUsername() {
        return viewedUsername;
    }
}