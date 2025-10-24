package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.*;
import com.github.chipolaris.bootforum2.dto.RankedListItemDTO;
import com.github.chipolaris.bootforum2.dto.admin.CountPerMonthDTO;
import com.github.chipolaris.bootforum2.test.DataJpaTestWithApplicationMocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTestWithApplicationMocks
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    // Common entities for tests that need them
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
    void whenExistsByUsername_andUserExists_thenReturnTrue() {
        // given
        createUser("testuser", "test@example.com", 0);

        // when
        boolean result = userRepository.existsByUsername("testuser");

        // then
        assertThat(result).isTrue();
    }

    @Test
    void whenExistsByUsername_andUserDoesNotExist_thenReturnFalse() {
        // when
        boolean result = userRepository.existsByUsername("nonexistent");

        // then
        assertThat(result).isFalse();
    }

    @Test
    void whenFindByUsername_andUserExists_thenReturnUser() {
        // given
        User user = createUser("testuser", "test@example.com", 0);

        // when
        Optional<User> found = userRepository.findByUsername("testuser");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(user.getId());
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    void whenFindByUsername_andUserDoesNotExist_thenReturnEmpty() {
        // when
        Optional<User> found = userRepository.findByUsername("nonexistent");

        // then
        assertThat(found).isNotPresent();
    }

    @Test
    void testFindTopUsersByDiscussionCount() {
        // given
        User user1 = createUser("user1", "user1@example.com", 0); // Will have 2 discussions
        User user2 = createUser("user2", "user2@example.com", 0); // Will have 1 discussion
        createUser("user3", "user3@example.com", 0); // Will have 0 discussions

        createDiscussion(user1, "D1", LocalDateTime.now().minusDays(1));
        createDiscussion(user1, "D2", LocalDateTime.now().minusDays(2));
        createDiscussion(user2, "D3", LocalDateTime.now().minusDays(3));
        // This one is old and should be excluded
        createDiscussion(user1, "D4-old", LocalDateTime.now().minusDays(40));

        entityManager.flush();

        // when
        List<RankedListItemDTO> results = userRepository.findTopUsersByDiscussionCount(
                LocalDateTime.now().minusDays(30), PageRequest.of(0, 5));

        // then
        assertThat(results).hasSize(2);
        // user1 should be first with 2 discussions
        assertThat(results.get(0).id()).isEqualTo(user1.getId());
        assertThat(results.get(0).name()).isEqualTo("user1");
        assertThat(results.get(0).value()).isEqualTo(2);
        // user2 should be second with 1 discussion
        assertThat(results.get(1).id()).isEqualTo(user2.getId());
        assertThat(results.get(1).name()).isEqualTo("user2");
        assertThat(results.get(1).value()).isEqualTo(1);
    }

    @Test
    void testFindTopUsersByCommentCount() {
        // given
        User user1 = createUser("user1", "user1@example.com", 0); // 2 comments
        User user2 = createUser("user2", "user2@example.com", 0); // 1 comment
        Discussion discussion = createDiscussion(user1, "Test Discussion", LocalDateTime.now());

        createComment(user1, discussion, "C1", LocalDateTime.now().minusDays(1));
        createComment(user1, discussion, "C2", LocalDateTime.now().minusDays(2));
        createComment(user2, discussion, "C3", LocalDateTime.now().minusDays(3));
        // This one is old and should be excluded
        createComment(user1, discussion, "C4-old", LocalDateTime.now().minusDays(40));

        entityManager.flush();

        // when
        List<RankedListItemDTO> results = userRepository.findTopUsersByCommentCount(
                LocalDateTime.now().minusDays(30), PageRequest.of(0, 5));

        // then
        assertThat(results).hasSize(2);
        // user1 should be first with 2 comments
        assertThat(results.get(0).id()).isEqualTo(user1.getId());
        assertThat(results.get(0).name()).isEqualTo("user1");
        assertThat(results.get(0).value()).isEqualTo(2);
        // user2 should be second with 1 comment
        assertThat(results.get(1).id()).isEqualTo(user2.getId());
        assertThat(results.get(1).name()).isEqualTo("user2");
        assertThat(results.get(1).value()).isEqualTo(1);
    }

    @Test
    void testCountByMonth() {
        // given
        createUser("userJan", "jan@example.com", 0, LocalDateTime.of(2024, 1, 15, 0, 0));
        createUser("userFeb1", "feb1@example.com", 0, LocalDateTime.of(2024, 2, 10, 0, 0));
        createUser("userFeb2", "feb2@example.com", 0, LocalDateTime.of(2024, 2, 20, 0, 0));
        // This one is old and should be excluded
        createUser("userOld", "old@example.com", 0, LocalDateTime.of(2023, 12, 31, 0, 0));

        entityManager.flush();

        // when
        List<CountPerMonthDTO> results = userRepository.countByMonth(LocalDateTime.of(2024, 1, 1, 0, 0));

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
    void testFindTopUsersByReputation() {
        // given
        User user1 = createUser("user1", "user1@example.com", 100);
        User user2 = createUser("user2", "user2@example.com", 500);
        User user3 = createUser("user3", "user3@example.com", 50);

        entityManager.flush();

        // when
        List<RankedListItemDTO> results = userRepository.findTopUsersByReputation(PageRequest.of(0, 5));

        // then
        assertThat(results).hasSize(3);
        // user2 should be first with 500 reputation
        assertThat(results.get(0).id()).isEqualTo(user2.getId());
        assertThat(results.get(0).name()).isEqualTo("user2");
        assertThat(results.get(0).value()).isEqualTo(500);
        // user1 should be second
        assertThat(results.get(1).id()).isEqualTo(user1.getId());
        assertThat(results.get(1).value()).isEqualTo(100);
        // user3 should be last
        assertThat(results.get(2).id()).isEqualTo(user3.getId());
        assertThat(results.get(2).value()).isEqualTo(50);
    }

    // --- Helper Methods ---

    private User createUser(String username, String email, long reputation) {
        return createUser(username, email, reputation, LocalDateTime.now());
    }

    private User createUser(String username, String email, long reputation, LocalDateTime createDate) {
        User user = User.newUser();
        user.setUsername(username);
        user.setPassword("password");
        user.setCreateDate(createDate);
        user.getPerson().setEmail(email);
        user.getStat().setReputation(reputation);
        return entityManager.persist(user);
    }

    private Discussion createDiscussion(User author, String title, LocalDateTime createDate) {
        Discussion discussion = Discussion.newDiscussion();
        discussion.setForum(this.forum);
        discussion.setCreateBy(author.getUsername());
        discussion.setTitle(title);
        discussion.setCreateDate(createDate);
        return entityManager.persist(discussion);
    }

    private Comment createComment(User author, Discussion discussion, String title, LocalDateTime createDate) {
        Comment comment = new Comment();
        comment.setDiscussion(discussion);
        comment.setCreateBy(author.getUsername());
        comment.setTitle(title);
        comment.setCreateDate(createDate);
        return entityManager.persist(comment);
    }
}