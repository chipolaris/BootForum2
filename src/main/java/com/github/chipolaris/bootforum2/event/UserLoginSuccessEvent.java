package com.github.chipolaris.bootforum2.event; // Create this package if it doesn't exist

import org.springframework.context.ApplicationEvent;

public class UserLoginSuccessEvent extends ApplicationEvent {

    private final String username;

    public UserLoginSuccessEvent(Object source, String username) {
        super(source);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}