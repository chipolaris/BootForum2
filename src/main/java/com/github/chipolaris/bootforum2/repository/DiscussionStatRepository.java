package com.github.chipolaris.bootforum2.repository; // Or your preferred repository package

import com.github.chipolaris.bootforum2.domain.DiscussionStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscussionStatRepository extends JpaRepository<DiscussionStat, Long> {

    /**
     * Checks if a vote exists for a given DiscussionStat ID and voter name.
     * This method leverages Spring Data JPA's query derivation.
     * It translates to a query similar to:
     * SELECT ... FROM DiscussionStat ds JOIN ds.votes v WHERE ds.id = ?1 AND v.voterName = ?2
     *
     * @param discussionStatId The ID of the DiscussionStat entity.
     * @param voterName The name of the voter to check for.
     * @return true if a vote by the given voter exists for the specified DiscussionStat, false otherwise.
     */
    boolean existsByIdAndVotes_VoterName(Long discussionStatId, String voterName);

    // Alternatively, if you prefer to be more explicit or if the derived name becomes too complex,
    // you could use the @Query annotation:
    @Query("SELECT CASE WHEN COUNT(v.id) > 0 THEN TRUE ELSE FALSE END " +
           "FROM DiscussionStat ds JOIN ds.votes v " +
           "WHERE ds.id = :discussionStatId AND v.voterName = :voterName")
    boolean hasUserVotedOnDiscussionStat(@Param("discussionStatId") Long discussionStatId, @Param("voterName") String voterName);
}