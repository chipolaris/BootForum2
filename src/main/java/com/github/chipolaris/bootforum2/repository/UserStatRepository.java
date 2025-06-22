package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.UserStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UserStatRepository extends JpaRepository<UserStat, Long> {

    /**
     * Atomically adds a given value to the reputation of a UserStat entity associated with a specific username.
     * This is more efficient than a read-then-write operation.
     *
     * @param username The username of the User whose UserStat's reputation is to be incremented.
     * @param reputationToAdd The value to add to the current reputation (can be positive or negative).
     * @return The number of entities updated (should be 1 if a matching user is found).
     */
    @Modifying
    @Query("UPDATE UserStat us SET us.reputation = us.reputation + :reputationToAdd WHERE us.id = (SELECT u.stat.id FROM User u WHERE u.username = :username)")
    int addReputationByUsername(@Param("username") String username, @Param("reputationToAdd") long reputationToAdd);

    /**
     * Atomically adds a given value to the profile view count of a UserStat entity associated with a specific username.
     *
     * @param username The username of the User whose UserStat's profile view count is to be incremented.
     * @param viewCountToAdd The value to add to the current profile view count.
     * @return The number of entities updated (should be 1 if a matching user is found).
     */
    @Modifying
    @Query("UPDATE UserStat us SET us.profileViewed = us.profileViewed + :viewCountToAdd WHERE us.id = (SELECT u.stat.id FROM User u WHERE u.username = :username)")
    int addProfileViewedByUsername(@Param("username") String username, @Param("viewCountToAdd") long viewCountToAdd);

    /**
     * Updates the lastLogin timestamp for a UserStat entity associated with a specific username to the current time.
     *
     * @param username The username of the User whose UserStat is to be updated.
     * @return The number of entities updated (should be 1 if a matching user is found).
     */
    @Modifying
    @Query("UPDATE UserStat us SET us.lastLogin = CURRENT_TIMESTAMP WHERE us.id = (SELECT u.stat.id FROM User u WHERE u.username = :username)")
    int updateLastLoginToNowByUsername(@Param("username") String username);
}