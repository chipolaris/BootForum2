package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.ForumSetting;
import com.github.chipolaris.bootforum2.test.DataJpaTestWithApplicationMocks;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTestWithApplicationMocks
public class ForumSettingRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ForumSettingRepository forumSettingRepository;

    @Test
    void whenFindByCategoryAndKeyName_andSettingExists_thenReturnSetting() {
        // given
        ForumSetting setting = ForumSetting.newInstance("content.posts", "minLength");
        setting.setValue("10");
        setting.setValueType("int");
        entityManager.persistAndFlush(setting);

        // when
        Optional<ForumSetting> found = forumSettingRepository.findByCategoryAndKeyName("content.posts", "minLength");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(setting.getId());
        assertThat(found.get().getValue()).isEqualTo("10");
    }

    @Test
    void whenFindByCategoryAndKeyName_andCategoryMismatch_thenReturnEmpty() {
        // given
        ForumSetting setting = ForumSetting.newInstance("content.posts", "minLength");
        setting.setValue("10");
        setting.setValueType("int");
        entityManager.persistAndFlush(setting);

        // when
        Optional<ForumSetting> found = forumSettingRepository.findByCategoryAndKeyName("content.images", "minLength");

        // then
        assertThat(found).isNotPresent();
    }

    @Test
    void whenFindByCategoryAndKeyName_andKeyNameMismatch_thenReturnEmpty() {
        // given
        ForumSetting setting = ForumSetting.newInstance("content.posts", "minLength");
        setting.setValue("10");
        setting.setValueType("int");
        entityManager.persistAndFlush(setting);

        // when
        Optional<ForumSetting> found = forumSettingRepository.findByCategoryAndKeyName("content.posts", "maxLength");

        // then
        assertThat(found).isNotPresent();
    }

    @Test
    void whenFindByCategoryAndKeyName_andNoSettingExists_thenReturnEmpty() {
        // when
        Optional<ForumSetting> found = forumSettingRepository.findByCategoryAndKeyName("non.existent", "key");

        // then
        assertThat(found).isNotPresent();
    }
}