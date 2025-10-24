package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.*;
import com.github.chipolaris.bootforum2.dto.ReplyToMyCommentDTO;
import com.github.chipolaris.bootforum2.dto.admin.CountPerMonthDTO;
import com.github.chipolaris.bootforum2.test.DataJpaTestWithApplicationMocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTestWithApplicationMocks
public class CommentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CommentRepository commentRepository;

    private Discussion discussion1;
    private Discussion discussion2;
    private Forum forum1;

    @BeforeEach
    void setup() {
        // Setup a hierarchy of entities to test against
        ForumGroup forumGroup = new ForumGroup();
        forumGroup.setTitle("Test Group");
        entityManager.persist(forumGroup);

        forum1 = Forum.newForum();
        forum1.setTitle("Test Forum 1");
        forum1.setForumGroup(forumGroup);
        entityManager.persist(forum1);

        discussion1 = Discussion.newDiscussion();
        discussion1.setTitle("Test Discussion 1");
        discussion1.setForum(forum1);
        discussion1.setCreateBy("user1");
        entityManager.persist(discussion1);

        discussion2 = Discussion.newDiscussion();
        discussion2.setTitle("Test Discussion 2");
        discussion2.setForum(forum1);
        discussion2.setCreateBy("user2");
        entityManager.persist(discussion2);
    }

    @Test
    void testFindTop10ByCreateByOrderByCreateDateDesc() {
        // given
        createAndPersistComment(discussion1, "user1", "Comment 1", LocalDateTime.now().minusDays(1));
        createAndPersistComment(discussion1, "user1", "Comment 2", LocalDateTime.now());
        createAndPersistComment(discussion1, "user2", "Comment 3", LocalDateTime.now()); // different user

        // when
        List<Comment> results = commentRepository.findTop10ByCreateByOrderByCreateDateDesc("user1");

        // then
        assertThat(results).hasSize(2);
        assertThat(results.get(0).getTitle()).isEqualTo("Comment 2"); // most recent first
        assertThat(results.get(1).getTitle()).isEqualTo("Comment 1");
    }

    @Test
    void testFindLatestByForum() {
        // given
        Comment oldComment = createAndPersistComment(discussion1, "user1", "Old Comment", LocalDateTime.now().minusDays(5));
        Comment newComment = createAndPersistComment(discussion2, "user2", "New Comment", LocalDateTime.now());

        // when
        Optional<Comment> latestCommentOpt = commentRepository.findLatestByForum(forum1);

        // then
        assertThat(latestCommentOpt).isPresent();
        assertThat(latestCommentOpt.get().getId()).isEqualTo(newComment.getId());
        assertThat(latestCommentOpt.get().getTitle()).isEqualTo("New Comment");
    }

    @Test
    void testCountByDiscussion() {
        // given
        createAndPersistComment(discussion1, "user1", "Comment 1", LocalDateTime.now());
        createAndPersistComment(discussion1, "user2", "Comment 2", LocalDateTime.now());
        createAndPersistComment(discussion2, "user1", "Comment 3", LocalDateTime.now()); // different discussion

        // when
        long count = commentRepository.countByDiscussion(discussion1);

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testGetCommentorMap() {
        // given
        createAndPersistComment(discussion1, "user1", "Comment A", LocalDateTime.now());
        createAndPersistComment(discussion1, "user1", "Comment B", LocalDateTime.now());
        createAndPersistComment(discussion1, "user2", "Comment C", LocalDateTime.now());
        createAndPersistComment(discussion2, "user1", "Comment D", LocalDateTime.now()); // different discussion

        // when
        Map<String, Integer> commentorMap = commentRepository.getCommentorMap(discussion1);

        // then
        assertThat(commentorMap).hasSize(2);
        assertThat(commentorMap).containsEntry("user1", 2);
        assertThat(commentorMap).containsEntry("user2", 1);
    }

    @Test
    void testFindRepliesToUserComments() {
        // given
        Comment myComment = createAndPersistComment(discussion1, "me", "My Original Comment", LocalDateTime.now().minusDays(1));
        Comment replyToMyComment = createAndPersistComment(discussion1, "other_user", "Reply to you", LocalDateTime.now());
        replyToMyComment.setReplyTo(myComment);
        entityManager.persist(replyToMyComment);

        Comment otherComment = createAndPersistComment(discussion1, "other_user", "Another comment", LocalDateTime.now());
        Comment replyToOther = createAndPersistComment(discussion1, "me", "My reply to them", LocalDateTime.now());
        replyToOther.setReplyTo(otherComment);
        entityManager.persist(replyToOther);

        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10);

        // when
        List<ReplyToMyCommentDTO> replies = commentRepository.findRepliesToUserComments("me", pageable);

        // then
        assertThat(replies).hasSize(1);
        ReplyToMyCommentDTO replyDTO = replies.get(0);
        assertThat(replyDTO.replyId()).isEqualTo(replyToMyComment.getId());
        assertThat(replyDTO.replyAuthor()).isEqualTo("other_user");
        assertThat(replyDTO.myCommentId()).isEqualTo(myComment.getId());
        assertThat(replyDTO.myCommentTitle()).isEqualTo("My Original Comment");
    }

    @Test
    void testSumVoteUpCountByCreateBy() {
        // given
        Comment comment1 = createAndPersistComment(discussion1, "test_user", "Comment 1", LocalDateTime.now());
        CommentVote vote1 = new CommentVote();
        vote1.setVoteUpCount(5);
        comment1.setCommentVote(vote1);
        entityManager.persist(comment1);

        Comment comment2 = createAndPersistComment(discussion1, "test_user", "Comment 2", LocalDateTime.now());
        CommentVote vote2 = new CommentVote();
        vote2.setVoteUpCount(3);
        comment2.setCommentVote(vote2);
        entityManager.persist(comment2);

        Comment comment3 = createAndPersistComment(discussion1, "other_user", "Comment 3", LocalDateTime.now());
        CommentVote vote3 = new CommentVote();
        vote3.setVoteUpCount(10);
        comment3.setCommentVote(vote3);
        entityManager.persist(comment3);

        entityManager.flush();

        // when
        Long totalUpVotes = commentRepository.sumVoteUpCountByCreateBy("test_user");

        // then
        assertThat(totalUpVotes).isEqualTo(8L);
    }

    @Test
    void testSumVoteUpCountByCreateBy_whenNoComments_shouldReturnNull() {
        // when
        Long totalUpVotes = commentRepository.sumVoteUpCountByCreateBy("non_existent_user");

        // then
        assertThat(totalUpVotes).isNull();
    }

    @Test
    void testCountPerMonthSince() {
        // given
        createAndPersistComment(discussion1, "user1", "Jan Comment", LocalDateTime.of(2024, 1, 15, 10, 0));
        createAndPersistComment(discussion1, "user1", "Feb Comment 1", LocalDateTime.of(2024, 2, 10, 10, 0));
        createAndPersistComment(discussion1, "user1", "Feb Comment 2", LocalDateTime.of(2024, 2, 20, 10, 0));
        createAndPersistComment(discussion1, "user1", "March Comment", LocalDateTime.of(2024, 3, 5, 10, 0));
        // A comment before the 'since' date
        createAndPersistComment(discussion1, "user1", "Old Comment", LocalDateTime.of(2023, 12, 31, 10, 0));

        // when
        List<CountPerMonthDTO> counts = commentRepository.countPerMonthSince(LocalDateTime.of(2024, 1, 1, 0, 0));

        // then
        assertThat(counts).hasSize(3);
        assertThat(counts.get(0).year()).isEqualTo(2024);
        assertThat(counts.get(0).month()).isEqualTo(1);
        assertThat(counts.get(0).count()).isEqualTo(1);

        assertThat(counts.get(1).year()).isEqualTo(2024);
        assertThat(counts.get(1).month()).isEqualTo(2);
        assertThat(counts.get(1).count()).isEqualTo(2);

        assertThat(counts.get(2).year()).isEqualTo(2024);
        assertThat(counts.get(2).month()).isEqualTo(3);
        assertThat(counts.get(2).count()).isEqualTo(1);
    }

    @Test
    void testGetReputationFromComments() {
        // given
        // User1: +1, -1 -> total 0
        Comment commentU1C1 = createAndPersistComment(discussion1, "user1", "C1", LocalDateTime.now());
        addVoteToComment(commentU1C1, "voterA", (short) 1);
        addVoteToComment(commentU1C1, "voterB", (short) -1);

        // User2: +1, +1 -> total 2
        Comment commentU2C1 = createAndPersistComment(discussion1, "user2", "C2", LocalDateTime.now());
        addVoteToComment(commentU2C1, "voterA", (short) 1);
        Comment commentU2C2 = createAndPersistComment(discussion1, "user2", "C3", LocalDateTime.now());
        addVoteToComment(commentU2C2, "voterC", (short) 1);

        // User3: -1 -> total -1
        Comment commentU3C1 = createAndPersistComment(discussion1, "user3", "C4", LocalDateTime.now());
        addVoteToComment(commentU3C1, "voterA", (short) -1);

        entityManager.flush();

        // when
        List<Object[]> reputations = commentRepository.getReputationFromComments();

        // then
        assertThat(reputations).hasSize(3);
        Map<String, Long> reputationMap = reputations.stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1]));

        assertThat(reputationMap).containsEntry("user1", 0L);
        assertThat(reputationMap).containsEntry("user2", 2L);
        assertThat(reputationMap).containsEntry("user3", -1L);
    }

    // Helper method to reduce boilerplate
    private Comment createAndPersistComment(Discussion discussion, String author, String title, LocalDateTime createDate) {
        Comment comment = new Comment();
        comment.setDiscussion(discussion);
        comment.setCreateBy(author);
        comment.setTitle(title);
        comment.setContent("Content for " + title);
        comment.setCreateDate(createDate);
        return entityManager.persist(comment);
    }

    private void addVoteToComment(Comment comment, String voterName, short value) {
        CommentVote commentVote = comment.getCommentVote();
        if (commentVote == null) {
            commentVote = new CommentVote();
            comment.setCommentVote(commentVote);
            commentVote.setVotes(new HashSet<>());
        }

        Vote vote = new Vote();
        vote.setVoterName(voterName);
        vote.setVoteValue(value);
        commentVote.getVotes().add(vote);

        if (value > 0) {
            commentVote.addVoteUpCount();
        } else {
            commentVote.addVoteDownCount();
        }
        entityManager.persist(comment);
    }
}