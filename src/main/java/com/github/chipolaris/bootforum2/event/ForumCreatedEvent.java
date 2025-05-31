package com.github.chipolaris.bootforum2.event;

import com.github.chipolaris.bootforum2.domain.Forum;
import org.springframework.context.ApplicationEvent;

public class ForumCreatedEvent extends ApplicationEvent {
    private final Forum forum;

    public ForumCreatedEvent(Object source, Forum forum) {
        super(source);
        this.forum = forum;
    }

    public Forum getForum() {
        return forum;
    }
}
