package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.Comment;
import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.domain.Forum;
import com.github.chipolaris.bootforum2.dto.DiscussionSummaryDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DiscussionRepository extends JpaRepository<Discussion, Long> {

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
     * Finds the most recent comment for a given discussion.
     *
     * @param forum The forum in which to find the latest discussion.
     * @return an Optional containing the latest Discussion in the forum, or empty if the forum has no discussions.
     */
    Optional<Discussion> findTopByForumOrderByCreateDateDesc(Forum forum);
}