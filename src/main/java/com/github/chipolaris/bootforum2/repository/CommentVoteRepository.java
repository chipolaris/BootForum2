package com.github.chipolaris.bootforum2.repository; // Or your preferred repository package

import com.github.chipolaris.bootforum2.domain.CommentVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentVoteRepository extends JpaRepository<CommentVote, Long> {

    /**
     * Checks if a vote exists for a given CommentVote ID and voter name.
     * This method leverages Spring Data JPA's query derivation.
     * It translates to a query similar to:
     * SELECT ... FROM CommentVote cv JOIN cv.votes v WHERE cv.id = ?1 AND v.voterName = ?2
     *
     * @param commentVoteId The ID of the CommentVote entity.
     * @param voterName The name of the voter to check for.
     * @return true if a vote by the given voter exists for the specified CommentVote, false otherwise.
     */
    boolean existsByIdAndVotes_VoterName(Long commentVoteId, String voterName);

    // Alternatively, if you prefer to be more explicit or if the derived name becomes too complex,
    // you could use the @Query annotation:
    @Query("SELECT CASE WHEN COUNT(v.id) > 0 THEN TRUE ELSE FALSE END " +
           "FROM CommentVote cv JOIN cv.votes v " +
           "WHERE cv.id = :commentVoteId AND v.voterName = :voterName")
    boolean hasUserVotedOnCommentVote(@Param("commentVoteId") Long commentVoteId, @Param("voterName") String voterName);
}