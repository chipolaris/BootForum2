package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.config.SeedDataInitializer;
import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.domain.Forum;
import com.github.chipolaris.bootforum2.domain.ForumGroup;
import com.github.chipolaris.bootforum2.dto.RankedListItemDTO;
import com.github.chipolaris.bootforum2.dto.admin.ForumActivityDTO;
import com.github.chipolaris.bootforum2.security.JwtAuthenticationFilter;
import com.github.chipolaris.bootforum2.service.ForumSettingService;
import com.github.chipolaris.bootforum2.service.SystemStatistic;
import com.github.chipolaris.bootforum2.test.DataJpaTestWithApplicationMocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTestWithApplicationMocks
public class ForumRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ForumRepository forumRepository;

    private ForumGroup forumGroup;

    @BeforeEach
    void setup() {
        forumGroup = new ForumGroup();
        forumGroup.setTitle("Test Group");
        entityManager.persist(forumGroup);
    }

    @Test
    void testFindTopForumsByViews() {
        // given
        Forum forum1 = createForum("Forum 1");
        Forum forum2 = createForum("Forum 2");

        // Discussions for Forum 1
        createDiscussion(forum1, "D1", 100, LocalDateTime.now().minusDays(1));
        createDiscussion(forum1, "D2", 50, LocalDateTime.now().minusDays(2));
        // This one is old and should be excluded
        createDiscussion(forum1, "D3", 999, LocalDateTime.now().minusDays(40));

        // Discussions for Forum 2
        createDiscussion(forum2, "D4", 200, LocalDateTime.now().minusDays(3));

        entityManager.flush();

        // when
        List<RankedListItemDTO> results = forumRepository.findTopForumsByViews(
                LocalDateTime.now().minusDays(30), PageRequest.of(0, 5));

        // then
        assertThat(results).hasSize(2);
        // Forum 2 should be first (200 views)
        assertThat(results.get(0).id()).isEqualTo(forum2.getId());
        assertThat(results.get(0).name()).isEqualTo("Forum 2");
        assertThat(results.get(0).value()).isEqualTo(200);
        // Forum 1 should be second (100 + 50 = 150 views)
        assertThat(results.get(1).id()).isEqualTo(forum1.getId());
        assertThat(results.get(1).name()).isEqualTo("Forum 1");
        assertThat(results.get(1).value()).isEqualTo(150);
    }

    @Test
    void testFindTopForumsByComments() {
        // given
        Forum forum1 = createForum("Forum 1");
        forum1.getStat().setCommentCount(100);
        forum1.getStat().getLastComment().setCommentDate(LocalDateTime.now().minusDays(1));

        Forum forum2 = createForum("Forum 2");
        forum2.getStat().setCommentCount(200);
        forum2.getStat().getLastComment().setCommentDate(LocalDateTime.now().minusDays(2));

        // This one is old and should be excluded
        Forum forum3 = createForum("Forum 3");
        forum3.getStat().setCommentCount(999);
        forum3.getStat().getLastComment().setCommentDate(LocalDateTime.now().minusDays(40));

        entityManager.flush();

        // when
        List<RankedListItemDTO> results = forumRepository.findTopForumsByComments(
                LocalDateTime.now().minusDays(30), PageRequest.of(0, 5));

        // then
        assertThat(results).hasSize(2);
        // Forum 2 has more comments and is recent
        assertThat(results.get(0).id()).isEqualTo(forum2.getId());
        assertThat(results.get(0).value()).isEqualTo(200);
        // Forum 1 has fewer comments and is recent
        assertThat(results.get(1).id()).isEqualTo(forum1.getId());
        assertThat(results.get(1).value()).isEqualTo(100);
    }

    @Test
    void testFindTopForumActivity() {
        // given
        Forum forum1 = createForum("Low Activity"); // 10 + 20 = 30
        forum1.getStat().setDiscussionCount(10);
        forum1.getStat().setCommentCount(20);

        Forum forum2 = createForum("High Activity"); // 50 + 50 = 100
        forum2.getStat().setDiscussionCount(50);
        forum2.getStat().setCommentCount(50);

        Forum forum3 = createForum("Medium Activity"); // 30 + 40 = 70
        forum3.getStat().setDiscussionCount(30);
        forum3.getStat().setCommentCount(40);

        entityManager.flush();

        // when
        List<ForumActivityDTO> results = forumRepository.findTopForumActivity(PageRequest.of(0, 3));

        // then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).forumName()).isEqualTo("High Activity");
        assertThat(results.get(0).discussionCount()).isEqualTo(50);
        assertThat(results.get(0).commentCount()).isEqualTo(50);

        assertThat(results.get(1).forumName()).isEqualTo("Medium Activity");
        assertThat(results.get(2).forumName()).isEqualTo("Low Activity");
    }

    // --- Helper Methods ---

    private Forum createForum(String title) {
        Forum forum = Forum.newForum();
        forum.setTitle(title);
        forum.setForumGroup(forumGroup);
        return entityManager.persist(forum);
    }



    private Discussion createDiscussion(Forum forum, String title, long viewCount, LocalDateTime createDate) {
        Discussion discussion = Discussion.newDiscussion();
        discussion.setForum(forum);
        discussion.setTitle(title);
        discussion.setCreateDate(createDate);
        discussion.getStat().setViewCount(viewCount);
        return entityManager.persist(discussion);
    }
}