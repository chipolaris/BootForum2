package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.domain.Forum;
import com.github.chipolaris.bootforum2.domain.ForumGroup;
import com.github.chipolaris.bootforum2.domain.Tag;
import com.github.chipolaris.bootforum2.dto.RankedListItemDTO;
import com.github.chipolaris.bootforum2.test.DataJpaTestWithApplicationMocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTestWithApplicationMocks
public class TagRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TagRepository tagRepository;

    private Forum forum;

    @BeforeEach
    void setup() {
        ForumGroup forumGroup = new ForumGroup();
        forumGroup.setTitle("Test Group");
        entityManager.persist(forumGroup);

        forum = Forum.newForum();
        forum.setTitle("Test Forum");
        forum.setForumGroup(forumGroup);
        entityManager.persist(forum);
    }

    @Test
    void testFindAllByOrderBySortOrderAsc() {
        // given
        createTag("java", 2);
        createTag("spring", 1);
        createTag("docker", 3);

        entityManager.flush();

        // when
        List<Tag> results = tagRepository.findAllByOrderBySortOrderAsc();

        // then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getLabel()).isEqualTo("spring");
        assertThat(results.get(1).getLabel()).isEqualTo("java");
        assertThat(results.get(2).getLabel()).isEqualTo("docker");
    }

    @Test
    void testFindTopTagsByViews() {
        // given
        Tag tagJava = createTag("java", 1);
        Tag tagSpring = createTag("spring", 2);

        // Discussions for 'java' tag
        createDiscussionWithTags("Java Intro", Set.of(tagJava), 100, 0, LocalDateTime.now().minusDays(1));
        createDiscussionWithTags("Java Advanced", Set.of(tagJava), 50, 0, LocalDateTime.now().minusDays(2));

        // Discussions for 'spring' tag
        createDiscussionWithTags("Spring Boot", Set.of(tagSpring), 200, 0, LocalDateTime.now().minusDays(3));

        // Old discussion that should be filtered out
        createDiscussionWithTags("Old Java Topic", Set.of(tagJava), 999, 0, LocalDateTime.now().minusDays(40));

        entityManager.flush();

        // when
        List<RankedListItemDTO> results = tagRepository.findTopTagsByViews(
                LocalDateTime.now().minusDays(30), PageRequest.of(0, 5));

        // then
        assertThat(results).hasSize(2);
        // 'spring' should be first with 200 views
        assertThat(results.get(0).name()).isEqualTo("spring");
        assertThat(results.get(0).value()).isEqualTo(200);
        // 'java' should be second with 100 + 50 = 150 views
        assertThat(results.get(1).name()).isEqualTo("java");
        assertThat(results.get(1).value()).isEqualTo(150);
    }

    @Test
    void testFindTopTagsByComments() {
        // given
        Tag tagJava = createTag("java", 1);
        Tag tagSpring = createTag("spring", 2);

        // Discussions for 'java' tag
        createDiscussionWithTags("Java Intro", Set.of(tagJava), 0, 10, LocalDateTime.now().minusDays(1));
        createDiscussionWithTags("Java Advanced", Set.of(tagJava), 0, 20, LocalDateTime.now().minusDays(2));

        // Discussions for 'spring' tag
        createDiscussionWithTags("Spring Boot", Set.of(tagSpring), 0, 50, LocalDateTime.now().minusDays(3));

        // Old discussion that should be filtered out
        createDiscussionWithTags("Old Java Topic", Set.of(tagJava), 0, 999, LocalDateTime.now().minusDays(40));

        entityManager.flush();

        // when
        List<RankedListItemDTO> results = tagRepository.findTopTagsByComments(
                LocalDateTime.now().minusDays(30), PageRequest.of(0, 5));

        // then
        assertThat(results).hasSize(2);
        // 'spring' should be first with 50 comments
        assertThat(results.get(0).name()).isEqualTo("spring");
        assertThat(results.get(0).value()).isEqualTo(50);
        // 'java' should be second with 10 + 20 = 30 comments
        assertThat(results.get(1).name()).isEqualTo("java");
        assertThat(results.get(1).value()).isEqualTo(30);
    }

    // --- Helper Methods ---

    private Tag createTag(String label, Integer sortOrder) {
        Tag tag = new Tag();
        tag.setLabel(label);
        tag.setSortOrder(sortOrder);
        return entityManager.persist(tag);
    }

    private void createDiscussionWithTags(String title, Set<Tag> tags, long viewCount, long commentCount, LocalDateTime createDate) {
        Discussion discussion = Discussion.newDiscussion();
        discussion.setForum(forum);
        discussion.setTitle(title);
        discussion.setCreateDate(createDate);
        discussion.setTags(tags);
        discussion.getStat().setViewCount(viewCount);
        discussion.getStat().setCommentCount(commentCount);
        entityManager.persist(discussion);
    }
}