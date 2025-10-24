package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.domain.DiscussionStat;
import com.github.chipolaris.bootforum2.domain.Vote;
import com.github.chipolaris.bootforum2.test.DataJpaTestWithApplicationMocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTestWithApplicationMocks
public class DiscussionStatRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DiscussionStatRepository discussionStatRepository;

    private DiscussionStat discussionStat;

    @BeforeEach
    void setup() {
        // Create a discussion which will cascade-create a DiscussionStat
        Discussion discussion = Discussion.newDiscussion();
        discussion.setTitle("Test Discussion");
        entityManager.persist(discussion);

        this.discussionStat = discussion.getStat();
    }

    @Test
    void whenExistsByIdAndVotes_VoterName_andVoteExists_thenReturnTrue() {
        // given
        addVoteToStat(discussionStat, "voter1");
        entityManager.flush();

        // when
        boolean result = discussionStatRepository.existsByIdAndVotes_VoterName(discussionStat.getId(), "voter1");

        // then
        assertThat(result).isTrue();
    }

    @Test
    void whenExistsByIdAndVotes_VoterName_andVoteDoesNotExist_thenReturnFalse() {
        // given
        addVoteToStat(discussionStat, "voter1");
        entityManager.flush();

        // when
        boolean result = discussionStatRepository.existsByIdAndVotes_VoterName(discussionStat.getId(), "non_voter");

        // then
        assertThat(result).isFalse();
    }

    @Test
    void whenExistsByIdAndVotes_VoterName_andStatDoesNotExist_thenReturnFalse() {
        // when
        boolean result = discussionStatRepository.existsByIdAndVotes_VoterName(9999L, "voter1");

        // then
        assertThat(result).isFalse();
    }

    @Test
    void whenHasUserVotedOnDiscussionStat_andVoteExists_thenReturnTrue() {
        // given
        addVoteToStat(discussionStat, "voter1");
        entityManager.flush();

        // when
        boolean result = discussionStatRepository.hasUserVotedOnDiscussionStat(discussionStat.getId(), "voter1");

        // then
        assertThat(result).isTrue();
    }

    @Test
    void whenHasUserVotedOnDiscussionStat_andVoteDoesNotExist_thenReturnFalse() {
        // given
        addVoteToStat(discussionStat, "voter1");
        entityManager.flush();

        // when
        boolean result = discussionStatRepository.hasUserVotedOnDiscussionStat(discussionStat.getId(), "non_voter");

        // then
        assertThat(result).isFalse();
    }

    @Test
    void whenHasUserVotedOnDiscussionStat_andStatDoesNotExist_thenReturnFalse() {
        // when
        boolean result = discussionStatRepository.hasUserVotedOnDiscussionStat(9999L, "voter1");

        // then
        assertThat(result).isFalse();
    }

    /**
     * Helper method to add a vote to a DiscussionStat.
     * The CascadeType.ALL on the 'votes' collection in DiscussionStat ensures the new Vote is persisted.
     */
    private void addVoteToStat(DiscussionStat stat, String voterName) {
        if (stat.getVotes() == null) {
            stat.setVotes(new HashSet<>());
        }
        Vote vote = new Vote();
        vote.setVoterName(voterName);
        vote.setVoteValue((short) 1);

        stat.getVotes().add(vote);

        entityManager.persist(stat);
    }
}