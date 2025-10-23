package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.config.SeedDataInitializer;
import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.domain.Avatar;
import com.github.chipolaris.bootforum2.domain.FileInfo;
import com.github.chipolaris.bootforum2.security.JwtAuthenticationFilter;
import com.github.chipolaris.bootforum2.service.ForumSettingService;
import com.github.chipolaris.bootforum2.service.SystemStatistic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest // Configures an in-memory H2 database and loads only JPA components
public class AvatarRepositoryTest {

    // Add a mocked beans so that {@link SpringBootAngularApplication.java} can inject them
    // during test context loading for this slice test.
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private SeedDataInitializer seedDataInitializer;

    @MockitoBean
    private DynamicDAO dynamicDAO;

    @MockitoBean
    private ForumSettingService forumSettingService;

    @MockitoBean
    private SystemStatistic systemStatistic;
    // end mocked beans

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AvatarRepository avatarRepository;

    @Test
    void whenFindByUserName_andAvatarExists_thenReturnAvatar() {
        // given
        FileInfo fileInfo = new FileInfo();
        fileInfo.setPath("/path/to/avatar.png");

        Avatar avatar = new Avatar();
        avatar.setUserName("testuser");
        avatar.setFile(fileInfo);

        entityManager.persist(avatar);
        entityManager.flush();

        // when
        Optional<Avatar> found = avatarRepository.findByUserName("testuser");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getUserName()).isEqualTo("testuser");
        assertThat(found.get().getFile().getPath()).isEqualTo("/path/to/avatar.png");
    }

    @Test
    void whenFindByUserName_andAvatarDoesNotExist_thenReturnEmpty() {
        // when
        Optional<Avatar> found = avatarRepository.findByUserName("nonexistentuser");

        // then
        assertThat(found).isNotPresent();
    }

    @Test
    void whenFindAvatarFileIdsByUserNames_withMixedUsers_thenReturnMapWithNulls() {
        // given
        FileInfo fileInfo1 = new FileInfo();
        fileInfo1.setPath("/path/1");
        Avatar avatar1 = new Avatar();
        avatar1.setUserName("user_with_avatar1");
        avatar1.setFile(fileInfo1);
        entityManager.persist(avatar1);

        FileInfo fileInfo2 = new FileInfo();
        fileInfo2.setPath("/path/2");
        Avatar avatar2 = new Avatar();
        avatar2.setUserName("user_with_avatar2");
        avatar2.setFile(fileInfo2);
        entityManager.persist(avatar2);

        entityManager.flush();

        List<String> userNames = Arrays.asList("user_with_avatar1", "user_without_avatar", "user_with_avatar2");

        // when
        Map<String, Long> avatarFileIds = avatarRepository.findAvatarFileIdsByUserNames(userNames);

        // then
        assertThat(avatarFileIds).isNotNull();
        assertThat(avatarFileIds).hasSize(3);
        assertThat(avatarFileIds).containsKeys("user_with_avatar1", "user_without_avatar", "user_with_avatar2");

        assertThat(avatarFileIds.get("user_with_avatar1")).isEqualTo(fileInfo1.getId());
        assertThat(avatarFileIds.get("user_with_avatar2")).isEqualTo(fileInfo2.getId());
        assertThat(avatarFileIds.get("user_without_avatar")).isNull();
    }

    @Test
    void whenFindAvatarFileIdsByUserNames_withOnlyUsersWithAvatars_thenReturnCompleteMap() {
        // given
        FileInfo fileInfo1 = new FileInfo();
        fileInfo1.setPath("/path/1");
        Avatar avatar1 = new Avatar();
        avatar1.setUserName("user1");
        avatar1.setFile(fileInfo1);
        entityManager.persist(avatar1);

        FileInfo fileInfo2 = new FileInfo();
        fileInfo2.setPath("/path/2");
        Avatar avatar2 = new Avatar();
        avatar2.setUserName("user2");
        avatar2.setFile(fileInfo2);
        entityManager.persist(avatar2);

        entityManager.flush();

        List<String> userNames = Arrays.asList("user1", "user2");

        // when
        Map<String, Long> avatarFileIds = avatarRepository.findAvatarFileIdsByUserNames(userNames);

        // then
        assertThat(avatarFileIds).isNotNull();
        assertThat(avatarFileIds).hasSize(2);
        assertThat(avatarFileIds.get("user1")).isEqualTo(fileInfo1.getId());
        assertThat(avatarFileIds.get("user2")).isEqualTo(fileInfo2.getId());
    }

    @Test
    void whenFindAvatarFileIdsByUserNames_withOnlyUsersWithoutAvatars_thenReturnMapWithAllNulls() {
        // given
        List<String> userNames = Arrays.asList("no_avatar1", "no_avatar2");

        // when
        Map<String, Long> avatarFileIds = avatarRepository.findAvatarFileIdsByUserNames(userNames);

        // then
        assertThat(avatarFileIds).isNotNull();
        assertThat(avatarFileIds).hasSize(2);
        assertThat(avatarFileIds.get("no_avatar1")).isNull();
        assertThat(avatarFileIds.get("no_avatar2")).isNull();
    }

    @Test
    void whenFindAvatarFileIdsByUserNames_withDuplicateInputUsernames_thenReturnMapWithDistinctKeys() {
        // given
        FileInfo fileInfo1 = new FileInfo();
        fileInfo1.setPath("/path/1");
        Avatar avatar1 = new Avatar();
        avatar1.setUserName("user_with_avatar");
        avatar1.setFile(fileInfo1);
        entityManager.persist(avatar1);
        entityManager.flush();

        List<String> userNames = Arrays.asList("user_with_avatar", "user_without_avatar", "user_with_avatar");

        // when
        Map<String, Long> avatarFileIds = avatarRepository.findAvatarFileIdsByUserNames(userNames);

        // then
        assertThat(avatarFileIds).isNotNull();
        // The default method implementation correctly handles distinct usernames
        assertThat(avatarFileIds).hasSize(2);
        assertThat(avatarFileIds.get("user_with_avatar")).isEqualTo(fileInfo1.getId());
        assertThat(avatarFileIds.get("user_without_avatar")).isNull();
    }

    @Test
    void whenFindAvatarFileIdsByUserNames_withEmptyList_thenReturnEmptyMap() {
        // when
        Map<String, Long> avatarFileIds = avatarRepository.findAvatarFileIdsByUserNames(Collections.emptyList());

        // then
        assertThat(avatarFileIds).isNotNull();
        assertThat(avatarFileIds).isEmpty();
    }

    @Test
    void whenFindAvatarFileIdsByUserNames_withNullList_thenReturnEmptyMap() {
        // when
        Map<String, Long> avatarFileIds = avatarRepository.findAvatarFileIdsByUserNames(null);

        // then
        assertThat(avatarFileIds).isNotNull();
        assertThat(avatarFileIds).isEmpty();
    }
}