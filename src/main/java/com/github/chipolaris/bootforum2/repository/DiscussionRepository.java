package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.Comment;
import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.domain.Forum;
import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.dto.DiscussionSummaryDTO;
import com.github.chipolaris.bootforum2.dto.MyLikedDiscussionDTO;
import com.github.chipolaris.bootforum2.dto.MyRecentDiscussionDTO;
import com.github.chipolaris.bootforum2.dto.RankedDiscussionDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiscussionRepository extends JpaRepository<Discussion, Long> {

    /**
     * Find all discussions in the system
     * @param pageable
     * @return
     */
    @Query("""
            SELECT new com.github.chipolaris.bootforum2.dto.DiscussionSummaryDTO(d.id, d.title, 
                d.stat.commentCount, d.stat.viewCount, d.createDate, d.createBy, d.stat.lastComment.commentDate) 
            FROM Discussion d
            """)
    public List<DiscussionSummaryDTO> findDiscussionSummaries(Pageable pageable);

    /**
     * Find all discussions in a given forum
     * @param forumId
     * @param pageable
     * @return
     */
    @Query("""
            SELECT new com.github.chipolaris.bootforum2.dto.DiscussionSummaryDTO(d.id, d.title, 
                d.stat.commentCount, d.stat.viewCount, d.createDate, d.createBy, d.stat.lastComment.commentDate) 
            FROM Discussion d WHERE d.forum.id = :forumId
            """)
    public List<DiscussionSummaryDTO> findDiscussionSummariesByForumId(@Param("forumId") Long forumId, Pageable pageable);

    @Query("""
            SELECT COUNT(d)
            FROM Discussion d
            WHERE d.forum.id = :forumId
        """)
    long countDiscussionsByForumId(@Param("forumId") Long forumId);

    /**
     * Efficiently counts the number of discussions associated with a given forum.
     *
     * @param forum The forum entity to count discussions for.
     * @return A long representing the total number of discussions.
     */
    long countByForum(Forum forum);

    // New method to find latest discussions by a user
    List<Discussion> findTop5ByCreateByOrderByCreateDateDesc(String createBy);

    /**
     * Find the latest discussion in the system
     * @return
     */
    Optional<Discussion> findTopByOrderByCreateDateDesc();

    /**
     * Finds a list of the most recent discussions in the entire system, ordered by creation date.
     * The number of discussions returned is determined by the size of the Pageable parameter.
     *
     * @param pageable Use PageRequest.of(0, N) to get the top N discussions.
     * @return A list of the latest discussions.
     */
    List<Discussion> findByOrderByCreateDateDesc(Pageable pageable);

    /**
     * Finds the most recent discussion for a given forum.
     *
     * @param forum The forum in which to find the latest discussion.
     * @return an Optional containing the latest Discussion in the forum, or empty if the forum has no discussions.
     */
    Optional<Discussion> findTopByForumOrderByCreateDateDesc(Forum forum);

    /**
     * Finds a list of discussions with the most comments, ordered descending.
     * The number of discussions returned is determined by the size of the Pageable parameter.
     *
     * @param pageable Use PageRequest.of(0, N) to get the top N discussions.
     * @return A list of the most commented-on discussions.
     */
    List<Discussion> findByOrderByStatCommentCountDesc(Pageable pageable);

    /**
     * Finds a list of discussions with the most views, ordered descending.
     * The number of discussions returned is determined by the size of the Pageable parameter.
     *
     * @param pageable Use PageRequest.of(0, N) to get the top N discussions.
     * @return A list of the most viewed discussions.
     */
    List<Discussion> findByOrderByStatViewCountDesc(Pageable pageable);

    /**
     * Finds all "sticky" discussions for a given forum, ordered by the most recently updated.
     * This is typically used to display pinned discussions at the top of a forum view.
     *
     * @param forum The forum to search within.
     * @return A list of sticky discussions.
     */
    List<Discussion> findByForumAndStickyTrueOrderByUpdateDateDesc(Forum forum);

    /**
     * Finds all "important" discussions for a given forum, ordered by the most recently updated.
     * This can be used for announcements or other highlighted content.
     *
     * @param forum The forum to search within.
     * @return A list of important discussions.
     */
    List<Discussion> findByForumAndImportantTrueOrderByUpdateDateDesc(Forum forum);

    /**
     *
     * @param username
     * @param pageable
     * @return
     */
    @Query("""
            SELECT new com.github.chipolaris.bootforum2.dto.MyRecentDiscussionDTO(
                d.id, d.title, d.createDate, d.stat.lastComment.title, d.stat.lastComment.commentDate)
            FROM Discussion d WHERE d.createBy = :username
            """)
    List<MyRecentDiscussionDTO> findRecentDiscussionsForUser(@Param("username") String username, Pageable pageable);

    /**
     *
     * @param username
     * @param pageable
     * @return
     */
    @Query("""
            SELECT new com.github.chipolaris.bootforum2.dto.MyLikedDiscussionDTO(
                d.id, d.title, d.createBy, v.createDate)
            FROM Discussion d JOIN d.stat.votes v WHERE v.voterName = :username AND v.voteValue > 0
            """)
    List<MyLikedDiscussionDTO> findLikedDiscussionsByUser(@Param("username") String username, Pageable pageable);

    /**
     *
     * @param username
     * @return
     */
    long countByCreateBy(String username);

    // ---- reputation queries
    @Query("SELECT SUM(d.stat.voteUpCount) FROM Discussion d WHERE d.createBy = :username")
    Long sumVoteUpCountByCreateBy(@Param("username") String username);

    @Query("SELECT SUM(d.stat.voteDownCount) FROM Discussion d WHERE d.createBy = :username")
    Long sumVoteDownCountByCreateBy(@Param("username") String username);

    @Query("SELECT new com.github.chipolaris.bootforum2.dto.RankedDiscussionDTO(d.id, d.title, d.stat.viewCount) FROM Discussion d WHERE d.createBy = :username ORDER BY d.stat.viewCount DESC")
    List<RankedDiscussionDTO> findMostViewedDiscussionsForUser(@Param("username") String username, Pageable pageable);

    @Query("SELECT new com.github.chipolaris.bootforum2.dto.RankedDiscussionDTO(d.id, d.title, CAST(d.stat.voteUpCount AS long)) FROM Discussion d WHERE d.createBy = :username ORDER BY d.stat.voteUpCount DESC")
    List<RankedDiscussionDTO> findMostLikedDiscussionsForUser(@Param("username") String username, Pageable pageable);

    @Query("SELECT new com.github.chipolaris.bootforum2.dto.RankedDiscussionDTO(d.id, d.title, CAST(d.stat.voteDownCount AS long)) FROM Discussion d WHERE d.createBy = :username ORDER BY d.stat.voteDownCount DESC")
    List<RankedDiscussionDTO> findMostDislikedDiscussionsForUser(@Param("username") String username, Pageable pageable);

    @Query("""
            SELECT new com.github.chipolaris.bootforum2.dto.RankedDiscussionDTO(d.id, d.title, CAST((d.stat.voteUpCount - d.stat.voteDownCount) AS long)) 
            FROM Discussion d WHERE d.createBy = :username 
            ORDER BY (d.stat.voteUpCount - d.stat.voteDownCount) DESC
            """)
    List<RankedDiscussionDTO> findMostNetLikedDiscussionsForUser(@Param("username") String username, Pageable pageable);

}