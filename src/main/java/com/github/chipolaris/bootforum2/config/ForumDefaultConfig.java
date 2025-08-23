package com.github.chipolaris.bootforum2.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix="forum")
@PropertySource(value = "classpath:forum-defaults.yml", factory = YamlPropertySourceFactory.class)
public class ForumDefaultConfig {

    // Map<String,Object> fields for categories
    /**
     * Using Map<String, Object> keeps it flexible
     * so we don’t need to hardcode nested classes for every category.
     */
    private Map<String, Object> general;
    private Map<String, Object> users;
    private Map<String, Object> content;
    private Map<String, Object> moderation;
    private Map<String, Object> images;
    private Map<String, Object> attachments;
    private Map<String, Object> notifications;
    private Map<String, Object> analytics;
    private Map<String, Object> system;

    public Map<String, Object> getGeneral() { return general;}
    public void setGeneral(Map<String, Object> general) {
        this.general = general;
    }

    public Map<String, Object> getUsers() {
        return users;
    }
    public void setUsers(Map<String, Object> users) {
        this.users = users;
    }

    public Map<String, Object> getContent() {
        return content;
    }
    public void setContent(Map<String, Object> content) {
        this.content = content;
    }

    public Map<String, Object> getModeration() {
        return moderation;
    }
    public void setModeration(Map<String, Object> moderation) {
        this.moderation = moderation;
    }

    public Map<String, Object> getImages() {
        return images;
    }
    public void setImages(Map<String, Object> images) {
        this.images = images;
    }

    public Map<String, Object> getAttachments() {
        return attachments;
    }
    public void setAttachments(Map<String, Object> attachments) {
        this.attachments = attachments;
    }

    public Map<String, Object> getNotifications() {
        return notifications;
    }
    public void setNotifications(Map<String, Object> notifications) {
        this.notifications = notifications;
    }

    public Map<String, Object> getAnalytics() {
        return analytics;
    }
    public void setAnalytics(Map<String, Object> analytics) {
        this.analytics = analytics;
    }

    public Map<String, Object> getSystem() {
        return system;
    }
    public void setSystem(Map<String, Object> system) {
        this.system = system;
    }
}
