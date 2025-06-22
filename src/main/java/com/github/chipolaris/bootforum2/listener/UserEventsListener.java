package com.github.chipolaris.bootforum2.listener; // Create this package if it doesn't exist

import com.github.chipolaris.bootforum2.event.UserLoginSuccessEvent;
import com.github.chipolaris.bootforum2.event.UserProfileViewedEvent;
import com.github.chipolaris.bootforum2.repository.UserStatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Component to listen to User events
 */
@Component
public class UserEventsListener {

    private static final Logger logger = LoggerFactory.getLogger(UserEventsListener.class);

    private final UserStatRepository userStatRepository;

    public UserEventsListener(UserStatRepository userStatRepository) {
        this.userStatRepository = userStatRepository;
    }

    @EventListener
    @Transactional(readOnly = false) // Ensure the update is part of a transaction
    @Async
    public void handleUserLoginSuccess(UserLoginSuccessEvent event) {
        String username = event.getUsername();
        logger.info("Handling UserLoginSuccessEvent for user: %s".formatted(username));

        int updatedRows = userStatRepository.updateLastLoginToNowByUsername(username);

        if(updatedRows == 0) {
            logger.warn("Not able to update lastLogin for username '%s'".formatted(username));
        }
    }

    @EventListener
    @Transactional(readOnly = false) // Ensure the update is part of a transaction
    @Async
    public void handleUserProfileViewed(UserProfileViewedEvent event) {
        String viewedUsername = event.getViewedUsername();
        logger.info("Handling UserProfileViewedEvent for user: %s".formatted(viewedUsername));

        // add 1 to user profile viewed count
        int updatedRows = userStatRepository.addProfileViewedByUsername(viewedUsername, (short) 1);

        if (updatedRows == 0) {
            logger.warn("Not able to update profile viewed count for username '%s'".formatted(viewedUsername));
        }
    }
}