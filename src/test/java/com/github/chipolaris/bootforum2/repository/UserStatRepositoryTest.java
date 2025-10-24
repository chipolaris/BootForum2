package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.test.DataJpaTestWithApplicationMocks;

import com.github.chipolaris.bootforum2.config.SeedDataInitializer;
import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.domain.UserStat;
import com.github.chipolaris.bootforum2.security.JwtAuthenticationFilter;
import com.github.chipolaris.bootforum2.service.ForumSettingService;
import com.github.chipolaris.bootforum2.service.SystemStatistic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTestWithApplicationMocks
public class UserStatRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserStatRepository userStatRepository;

    private User testUser;

    @BeforeEach
    void setup() {
        testUser = User.newUser();
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.getStat().setReputation(100L);
        testUser.getStat().setProfileViewed(50L);
        testUser.getStat().setLastLogin(null);

        entityManager.persist(testUser);
        entityManager.flush();
    }

    @Test
    void whenAddReputationByUsername_andUserExists_thenReputationIsUpdated() {
        // given
        long initialReputation = testUser.getStat().getReputation();
        long reputationToAdd = 25;

        // when
        int updatedRows = userStatRepository.addReputationByUsername("testuser", reputationToAdd);

        // then
        assertThat(updatedRows).isEqualTo(1);

        // Clear the persistence context to force a reload from the database
        entityManager.flush();
        entityManager.clear();

        User updatedUser = entityManager.find(User.class, testUser.getId());
        assertThat(updatedUser.getStat().getReputation()).isEqualTo(initialReputation + reputationToAdd);
    }

    @Test
    void whenAddReputationByUsername_andUserDoesNotExist_thenReturnZero() {
        // when
        int updatedRows = userStatRepository.addReputationByUsername("nonexistent", 10);

        // then
        assertThat(updatedRows).isEqualTo(0);
    }

    @Test
    void whenAddProfileViewedByUsername_andUserExists_thenViewCountIsUpdated() {
        // given
        long initialViews = testUser.getStat().getProfileViewed();

        // when
        int updatedRows = userStatRepository.addProfileViewedByUsername("testuser", 1);

        // then
        assertThat(updatedRows).isEqualTo(1);

        entityManager.flush();
        entityManager.clear();

        User updatedUser = entityManager.find(User.class, testUser.getId());
        assertThat(updatedUser.getStat().getProfileViewed()).isEqualTo(initialViews + 1);
    }

    @Test
    void whenAddProfileViewedByUsername_andUserDoesNotExist_thenReturnZero() {
        // when
        int updatedRows = userStatRepository.addProfileViewedByUsername("nonexistent", 1);

        // then
        assertThat(updatedRows).isEqualTo(0);
    }


    @Test
    void whenUpdateLastLoginToNowByUsername_andUserExists_thenLastLoginIsUpdated() {
        // given
        assertThat(testUser.getStat().getLastLogin()).isNull();
        LocalDateTime beforeUpdate = LocalDateTime.now();

        // when
        int updatedRows = userStatRepository.updateLastLoginToNowByUsername("testuser");

        // then
        assertThat(updatedRows).isEqualTo(1);

        entityManager.flush();
        entityManager.clear();

        User updatedUser = entityManager.find(User.class, testUser.getId());
        UserStat updatedStat = updatedUser.getStat();

        assertThat(updatedStat.getLastLogin()).isNotNull();
        assertThat(updatedStat.getLastLogin()).isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    void whenUpdateLastLoginToNowByUsername_andUserDoesNotExist_thenReturnZero() {
        // when
        int updatedRows = userStatRepository.updateLastLoginToNowByUsername("nonexistent");

        // then
        assertThat(updatedRows).isEqualTo(0);
    }
}