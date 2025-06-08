package com.github.chipolaris.bootforum2.event;

import com.github.chipolaris.bootforum2.domain.Comment;
import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.service.CommentService;
import org.springframework.context.ApplicationEvent;

public class CommentCreatedEvent extends ApplicationEvent {
    private final Comment comment;
    private final String username;

    public CommentCreatedEvent(Object source, Comment comment, String username) {
        super(source);
        this.comment = comment;
        this.username = username;
    }

    public Comment getComment() {
        return comment;
    }

    public String getUsername() {
        return username;
    }
}
