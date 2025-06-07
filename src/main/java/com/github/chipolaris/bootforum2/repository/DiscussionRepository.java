package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.domain.Discussion;
import com.github.chipolaris.bootforum2.dto.DiscussionSummaryDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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

}
