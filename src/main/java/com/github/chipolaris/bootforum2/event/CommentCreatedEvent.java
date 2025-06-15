package com.github.chipolaris.bootforum2.event;

import com.github.chipolaris.bootforum2.domain.Comment;
import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.service.CommentService;
import org.springframework.context.ApplicationEvent;

public class CommentCreatedEvent extends ApplicationEvent {
    private final Comment comment;

    public CommentCreatedEvent(Object source, Comment comment) {
        super(source);
        this.comment = comment;
    }

    public Comment getComment() {
        return comment;
    }
}
