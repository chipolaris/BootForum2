package com.github.chipolaris.bootforum2.listener; // Create this package if it doesn't exist

import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.GenericDAO;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.domain.UserStat;
import com.github.chipolaris.bootforum2.event.UserLoginSuccessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class UserLoginSuccessListener {

    private static final Logger logger = LoggerFactory.getLogger(UserLoginSuccessListener.class);

    private final GenericDAO genericDAO;
    private final DynamicDAO dynamicDAO;

    public UserLoginSuccessListener(GenericDAO genericDAO, DynamicDAO dynamicDAO) {
        this.genericDAO = genericDAO;
        this.dynamicDAO = dynamicDAO;
    }

    @EventListener
    @Transactional(readOnly = false) // Ensure the update is part of a transaction
    @Async
    public void handleUserLoginSuccess(UserLoginSuccessEvent event) {
        String username = event.getUsername();
        logger.info(String.format("Handling UserLoginSuccessEvent for user: %s", username));

        try {
            QuerySpec querySpec = QuerySpec.builder(User.class).filter(FilterSpec.eq("username", username)).build();
            User user = dynamicDAO.<User>findOptional(querySpec).orElse(null);

            if (user != null) {
                UserStat userStat = user.getStat();

                userStat.setLastLogin(LocalDateTime.now());

                genericDAO.merge(userStat);

                logger.info(String.format("Updated lastLogin for user: %s to %s", username, userStat.getLastLogin()));
            } else {
                logger.warn(String.format("User not found for username: %s", username));
            }
        } catch (Exception e) {
            logger.error(String.format("Error updating lastLogin for user %s: ", username), e);
        }
    }
}