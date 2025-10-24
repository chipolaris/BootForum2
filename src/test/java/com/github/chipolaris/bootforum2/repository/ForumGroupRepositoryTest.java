package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.config.SeedDataInitializer;
import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.domain.ForumGroup;
import com.github.chipolaris.bootforum2.security.JwtAuthenticationFilter;
import com.github.chipolaris.bootforum2.service.ForumSettingService;
import com.github.chipolaris.bootforum2.service.SystemStatistic;
import com.github.chipolaris.bootforum2.test.DataJpaTestWithApplicationMocks;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTestWithApplicationMocks
public class ForumGroupRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ForumGroupRepository forumGroupRepository;

    @Test
    void whenFindFirstByParentIsNull_andRootExists_thenReturnRoot() {
        // given
        ForumGroup rootGroup = new ForumGroup();
        rootGroup.setTitle("Root Group");
        rootGroup.setParent(null); // Explicitly a root group
        entityManager.persist(rootGroup);

        ForumGroup childGroup = new ForumGroup();
        childGroup.setTitle("Child Group");
        childGroup.setParent(rootGroup);
        entityManager.persist(childGroup);

        entityManager.flush();

        // when
        Optional<ForumGroup> found = forumGroupRepository.findFirstByParentIsNull();

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(rootGroup.getId());
        assertThat(found.get().getTitle()).isEqualTo("Root Group");
    }

    @Test
    void whenFindFirstByParentIsNull_andNoRootExists_thenReturnEmpty() {
        // given
        // No root group is created, only a child that references a non-persistent parent
        ForumGroup parent = new ForumGroup();
        // parent is not persisted

        ForumGroup childGroup = new ForumGroup();
        childGroup.setTitle("Orphan Group");
        childGroup.setParent(parent);
        // Persisting childGroup without its parent will result in parent_id being null,
        // so we don't persist anything to truly test the "not found" case.
        // The database is clean before this test.

        // when
        Optional<ForumGroup> found = forumGroupRepository.findFirstByParentIsNull();

        // then
        assertThat(found).isNotPresent();
    }
}