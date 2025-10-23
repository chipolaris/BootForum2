package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.config.SeedDataInitializer;
import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.domain.*;
import com.github.chipolaris.bootforum2.dto.MyLikedDiscussionDTO;
import com.github.chipolaris.bootforum2.dto.RankedListItemDTO;
import com.github.chipolaris.bootforum2.dto.admin.CountPerMonthDTO;
import com.github.chipolaris.bootforum2.security.JwtAuthenticationFilter;
import com.github.chipolaris.bootforum2.service.ForumSettingService;
import com.github.chipolaris.bootforum2.service.SystemStatistic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class DiscussionRepositoryTest {

    // Mock beans required by SpringBootAngularApplication to allow the test context to load
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

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DiscussionRepository discussionRepository;

    private Forum forum1;
    private Tag tag1;
    private Tag tag2;

    @BeforeEach
    void setup() {
        ForumGroup forumGroup = new ForumGroup();
        forumGroup.setTitle("Test Group");
        entityManager.persist(forumGroup);

        forum1 = Forum.newForum();
        forum1.setTitle("Test Forum");
        forum1.setForumGroup(forumGroup);
        entityManager.persist(forum1);

        tag1 = new Tag();
        tag1.setLabel("java");
        entityManager.persist(tag1);

        tag2 = new Tag();
        tag2.setLabel("spring");
        entityManager.persist(tag2);
    }

    @Test
    void testFindByTagIdsWithTags() {
        // given
        Discussion d1 = createAndPersistDiscussion(forum1, "userA", "Java Discussion", Set.of(tag1));
        Discussion d2 = createAndPersistDiscussion(forum1, "userB", "Spring Discussion", Set.of(tag2));
        Discussion d3 = createAndPersistDiscussion(forum1, "userC", "Unrelated Discussion", Set.of());

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Discussion> resultPage = discussionRepository.findByTagIdsWithTags(List.of(tag1.getId()), pageable);

        // then
        assertThat(resultPage.getContent()).hasSize(1);
        assertThat(resultPage.getContent().get(0).getId()).isEqualTo(d1.getId());
        // Eager fetch assertion
        assertThat(resultPage.getContent().get(0).getTags().iterator().next().getLabel()).isEqualTo("java");
    }

    @Test
    void testFindByOrderByStatCommentCountDesc() {
        // given
        Discussion d1 = createAndPersistDiscussion(forum1, "userA", "Discussion 1", Set.of());
        d1.getStat().setCommentCount(10);

        Discussion d2 = createAndPersistDiscussion(forum1, "userB", "Discussion 2", Set.of());
        d2.getStat().setCommentCount(50);

        Discussion d3 = createAndPersistDiscussion(forum1, "userC", "Discussion 3", Set.of());
        d3.getStat().setCommentCount(20);

        entityManager.flush();

        // when
        List<Discussion> results = discussionRepository.findByOrderByStatCommentCountDesc(PageRequest.of(0, 3));

        // then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getId()).isEqualTo(d2.getId());
        assertThat(results.get(1).getId()).isEqualTo(d3.getId());
        assertThat(results.get(2).getId()).isEqualTo(d1.getId());
    }

    @Test
    void testFindLikedDiscussionsByUser() {
        // given
        Discussion d1 = createAndPersistDiscussion(forum1, "author1", "Liked Discussion", Set.of());
        addVoteToDiscussion(d1, "liking_user", (short) 1);

        Discussion d2 = createAndPersistDiscussion(forum1, "author2", "Disliked Discussion", Set.of());
        addVoteToDiscussion(d2, "liking_user", (short) -1);

        Discussion d3 = createAndPersistDiscussion(forum1, "author3", "Other User Liked", Set.of());
        addVoteToDiscussion(d3, "other_user", (short) 1);

        entityManager.flush();

        // when
        List<MyLikedDiscussionDTO> results = discussionRepository.findLikedDiscussionsByUser("liking_user", PageRequest.of(0, 10));

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(d1.getId());
        assertThat(results.get(0).title()).isEqualTo("Liked Discussion");
    }

    @Test
    void testFindTopDiscussionsByViews() {
        // given
        Discussion d1 = createAndPersistDiscussion(forum1, "userA", "Popular", Set.of(), LocalDateTime.now().minusDays(1));
        d1.getStat().setViewCount(1000);

        Discussion d2 = createAndPersistDiscussion(forum1, "userB", "Less Popular", Set.of(), LocalDateTime.now().minusDays(2));
        d2.getStat().setViewCount(500);

        // This one is old and should be excluded
        Discussion d3 = createAndPersistDiscussion(forum1, "userC", "Very Old", Set.of(), LocalDateTime.now().minusDays(40));
        d3.getStat().setViewCount(9999);

        entityManager.flush();

        // when
        List<RankedListItemDTO> results = discussionRepository.findTopDiscussionsByViews(LocalDateTime.now().minusDays(30), PageRequest.of(0, 5));

        // then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).id()).isEqualTo(d1.getId());
        assertThat(results.get(0).name()).isEqualTo("Popular");
        assertThat(results.get(0).value()).isEqualTo(1000);
        assertThat(results.get(1).id()).isEqualTo(d2.getId());
    }

    @Test
    void testCountPerMonthSince() {
        // given
        createAndPersistDiscussion(forum1, "user", "Jan", Set.of(), LocalDateTime.of(2024, 1, 10, 0, 0));
        createAndPersistDiscussion(forum1, "user", "Feb 1", Set.of(), LocalDateTime.of(2024, 2, 5, 0, 0));
        createAndPersistDiscussion(forum1, "user", "Feb 2", Set.of(), LocalDateTime.of(2024, 2, 15, 0, 0));
        createAndPersistDiscussion(forum1, "user", "Old", Set.of(), LocalDateTime.of(2023, 12, 1, 0, 0));

        entityManager.flush();

        // when
        List<CountPerMonthDTO> results = discussionRepository.countPerMonthSince(LocalDateTime.of(2024, 1, 1, 0, 0));

        // then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).year()).isEqualTo(2024);
        assertThat(results.get(0).month()).isEqualTo(1);
        assertThat(results.get(0).count()).isEqualTo(1);
        assertThat(results.get(1).year()).isEqualTo(2024);
        assertThat(results.get(1).month()).isEqualTo(2);
        assertThat(results.get(1).count()).isEqualTo(2);
    }

    @Test
    void testGetReputationFromDiscussions() {
        // given
        Discussion d1 = createAndPersistDiscussion(forum1, "author1", "D1", Set.of());
        addVoteToDiscussion(d1, "voterA", (short) 1); // author1 gets +1
        addVoteToDiscussion(d1, "voterB", (short) 1); // author1 gets +1
        addVoteToDiscussion(d1, "voterC", (short) -1); // author1 gets -1. Total: 1

        Discussion d2 = createAndPersistDiscussion(forum1, "author2", "D2", Set.of());
        addVoteToDiscussion(d2, "voterA", (short) -1); // author2 gets -1. Total: -1

        Discussion d3 = createAndPersistDiscussion(forum1, "author1", "D3", Set.of());
        addVoteToDiscussion(d3, "voterD", (short) 1); // author1 gets +1. Total: 2

        entityManager.flush();

        // when
        List<Object[]> reputations = discussionRepository.getReputationFromDiscussions();
        Map<String, Long> reputationMap = reputations.stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1]));

        // then
        assertThat(reputationMap).hasSize(2);
        assertThat(reputationMap).containsEntry("author1", 2L);
        assertThat(reputationMap).containsEntry("author2", -1L);
    }

    // --- Helper Methods ---

    private Discussion createAndPersistDiscussion(Forum forum, String author, String title, Set<Tag> tags) {
        return createAndPersistDiscussion(forum, author, title, tags, LocalDateTime.now());
    }

    private Discussion createAndPersistDiscussion(Forum forum, String author, String title, Set<Tag> tags, LocalDateTime createDate) {
        Discussion discussion = Discussion.newDiscussion();
        discussion.setForum(forum);
        discussion.setCreateBy(author);
        discussion.setTitle(title);
        discussion.setContent("Content for " + title);
        discussion.setTags(tags);
        discussion.setCreateDate(createDate);
        return entityManager.persist(discussion);
    }

    private void addVoteToDiscussion(Discussion discussion, String voterName, short value) {
        DiscussionStat discussionStat = discussion.getStat();
        if (discussionStat.getVotes() == null) {
            discussionStat.setVotes(new HashSet<>());
        }

        Vote vote = new Vote();
        vote.setVoterName(voterName);
        vote.setVoteValue(value);
        discussionStat.getVotes().add(vote);

        if (value > 0) {
            discussionStat.addVoteUpCount();
        } else {
            discussionStat.addVoteDownCount();
        }
        entityManager.persist(discussion);
    }
}